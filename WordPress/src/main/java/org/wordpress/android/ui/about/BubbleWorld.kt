package org.wordpress.android.ui.about

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World

/**
 * This is the Box2d physics world.
 * It runs the simulation at a constant interval and notifies the listener at each simulation step.
 */
class BubbleWorld(private val listener: Listener) {
    /**
     * Listener to listen for simulation updates.
     */
    interface Listener {
        /**
         * This contains the updated values for a given [Bubble] after each simulation step.
         */
        fun onSimulationUpdate(bubble: Bubble)
    }

    /**
     * Describes the state of the 2d world.
     */
    enum class State {
        /**
         * The 2d world is not yet prepared. Call [create] before any other interaction with this instance.
         */
        IDLE,

        /**
         * The world is created and ready to start the simulation.
         */
        READY,

        /**
         * The world is simulating the movements of the bodies within it.
         */
        SIMULATING
    }

    /**
     * The current state of this instance.
     */
    var state = State.IDLE
        private set

    private companion object {
        /**
         * In order to define how frequent the simulation should be, we start by defining the desired FPS or our animation.
         */
        private const val TARGET_FPS = 60f

        /**
         * In order to get eg 60 FPS, the 2D world should run a simulation step every 1/60 seconds.
         */
        private const val TIME_STEP = 1.0f / TARGET_FPS

        /**
         * The above value represented in milliseconds and converted to long.
         */
        private const val TIME_STEP_MS = (TIME_STEP * 1000).toLong()

        /**
         * Defines how accurately the velocity should be calculated. Bigger the value means higher accuracy but slower performance.
         */
        private const val VELOCITY_ITERATIONS = 2

        /**
         * Defines how accurately the positions of the bodies should be calculated. Bigger the value means higher accuracy but slower performance.
         */
        private const val POSITION_ITERATIONS = 2

        /**
         * In this example we assume that all bodies have the same density.
         */
        private const val DENSITY = 0.5f

        /**
         * In this example we assume that all bodies have the same friction.
         */
        private const val FRICTION = 0.5f

        /**
         * Restitution defines how elastic the bodies are.
         * In this example we assume that all bodies have the same friction.
         */
        private const val RESTITUTION = 0.5f

        /**
         * The x value of the bubble's initial impulse.
         */
        private const val INITIAL_IMPULSE_X = 0.01f // 0.05f

        /**
         * The y value of the bubble's initial impulse.
         */
        private const val INITIAL_IMPULSE_Y = 0.01f // 0.05f
    }

    private val world = World(Vec2(0.0f, 0.0f), true)
    private val coroutineScope = CoroutineScope(SupervisorJob())
    private var worldWidth = 0f
    private var worldHeight = 0f

    /**
     * Create the 2D world.
     * If the world has already been created, then a subsequent call to this method will restart it.
     */
    fun create(viewWidth: Int, viewHeight: Int) {
        worldWidth = Metrics.pixelsToMeters(viewWidth.toFloat())
        worldHeight = Metrics.pixelsToMeters(viewHeight.toFloat())

        stopSimulation()
        destroyWorld()
        createWorldBoundaries()

        state = State.READY
    }

    /**
     * Add a bubble to the world.
     */
    fun createBubble(bubble: Bubble) {
        createBody(bubble)
    }

    /**
     * Start the simulation. If the simulations was already started before, then
     * a subsequent call to this method will simply stop it and restart it.
     */
    fun startSimulation() {
        stopSimulation()
        Log.d(BubbleWorld::class.java.simpleName, "Start simulation")
        coroutineScope.launch(Dispatchers.IO) {
            val timer = (0..Int.MAX_VALUE)
                    .asSequence()
                    .asFlow()
                    .onEach {
                        delay(TIME_STEP_MS)
                    }

            withContext(Dispatchers.Main) {
                timer.collect {
                    update()
                }
            }
        }

        state = State.SIMULATING
    }

    /**
     * Stops the simulation if it had already started.
     */
    fun stopSimulation() {
        Log.d(BubbleWorld::class.java.simpleName, "Stop simulation")
        state = State.READY
    }

    /**
     * Returns the world's body list. This is used for debugging purposes.
     */
    fun getBodyList(): Body? {
        return world.bodyList
    }

    /**
     * Brings the world to its initial state by destroying every single body that has been created.
     */
    private fun destroyWorld() {
        var body = world.bodyList
        while (body != null) {
            world.destroyBody(body)
            body = body.next
        }
    }

    /**
     * While the simulation is running, this method is called at constant intervals.
     */
    private fun update() {
        // Update Physics World
        world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS)
//        Log.d(BubbleWorld::class.java.simpleName, "Update")

        var body = world.bodyList

        while (body != null) {
            val bubble = body.userData
            if (bubble is Bubble) {
                // NOTE: In the 2D world (x,y) is the center of a body whereas
                // in the Android world (x,y) is the top left corner of a view.
                bubble.viewX = Metrics.metersToPixels(body.position.x) - bubble.viewSize / 2
                bubble.viewY = Metrics.metersToPixels(body.position.y) - bubble.viewSize / 2

                listener.onSimulationUpdate(bubble)
            }

            body = body.next
        }
    }

    /**
     * Creates the world's boundaries so that the bubbles don't move outside of it.
     */
    private fun createWorldBoundaries() {
        Log.d(BubbleWorld::class.java.simpleName, "Create Boundaries")
        val boundariesBodyDef = BodyDef()
        // A static body has 0 velocity and mass.
        boundariesBodyDef.type = BodyType.STATIC

        val topLeft = Vec2(0f, 0f)
        val topRight = Vec2(worldWidth, 0f)
        val bottomLeft = Vec2(0f, worldHeight)
        val bottomRight = Vec2(worldWidth, worldHeight)

        val leftShape = PolygonShape()
        val rightShape = PolygonShape()
        val topShape = PolygonShape()
        val bottomShape = PolygonShape()

        leftShape.setAsEdge(topLeft, bottomLeft)
        rightShape.setAsEdge(topRight, bottomRight)
        topShape.setAsEdge(topLeft, topRight)
        bottomShape.setAsEdge(bottomLeft, bottomRight)

        val leftFixture = FixtureDef()
        leftFixture.shape = leftShape
        leftFixture.restitution = RESTITUTION
        leftFixture.density = DENSITY
        leftFixture.friction = FRICTION

        val rightFixture = FixtureDef()
        rightFixture.shape = rightShape
        rightFixture.restitution = RESTITUTION
        rightFixture.density = DENSITY
        rightFixture.friction = FRICTION

        val topFixture = FixtureDef()
        topFixture.shape = topShape
        topFixture.restitution = RESTITUTION
        topFixture.density = DENSITY
        topFixture.friction = FRICTION

        val bottomFixture = FixtureDef()
        bottomFixture.shape = bottomShape
        bottomFixture.restitution = RESTITUTION
        bottomFixture.density = DENSITY
        bottomFixture.friction = FRICTION

        val boundariesBody = world.createBody(boundariesBodyDef)
        boundariesBody.createFixture(leftFixture)
        boundariesBody.createFixture(rightFixture)
        boundariesBody.createFixture(topFixture)
        boundariesBody.createFixture(bottomFixture)
    }

    /**
     * Creates a new circle in the 2D world with mass, velocity, etc.
     */
    private fun createBody(bubble: Bubble): Body {
        // Set the starting position anywhere within the world's boundaries.
        val startingX = (Math.random() * worldWidth).toFloat()
        val startingY = (Math.random() * worldHeight).toFloat()

        val bodyDef = BodyDef()
        bodyDef.type = BodyType.DYNAMIC
        bodyDef.position.set(startingX, startingY)

        val shape = CircleShape()
        shape.m_radius = Metrics.pixelsToMeters(bubble.viewSize / 2f)

        // Create a fixture for the bubble.
        val fixtureDef = FixtureDef()
        fixtureDef.shape = shape
        fixtureDef.density = DENSITY
        fixtureDef.friction = FRICTION
        fixtureDef.restitution = RESTITUTION

        val body = world.createBody(bodyDef)
        body.createFixture(fixtureDef)

        // We set the mass of the circle to be relative to its size.
        // A bubble with a bigger radius, will have a bigger mass.
        body.m_mass = Math.PI.toFloat() * shape.m_radius

        // Set the bubble in the user data so that we can easily retrieve it later and update the values that it holds.
        body.userData = bubble

        // Give the body an initial speed.
        val impulse = Vec2(INITIAL_IMPULSE_X, INITIAL_IMPULSE_Y)
        body.applyLinearImpulse(impulse, body.position)

        return body
    }
}
