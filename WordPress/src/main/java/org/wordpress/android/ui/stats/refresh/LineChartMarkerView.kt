package org.wordpress.android.ui.stats.refresh

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.BlurMaskFilter.Blur.NORMAL
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Path.Direction.CW
import android.graphics.RectF
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases.ViewsAndVisitorsMapper.SelectedType
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import javax.inject.Inject

@Suppress("MagicNumber")
@AndroidEntryPoint
class LineChartMarkerView @Inject constructor(
    context: Context,
    private val selectedType: Int
) : MarkerView(context, R.layout.stats_line_chart_marker) {
    @Inject
    lateinit var statsUtils: StatsUtils
    private val changeView = findViewById<TextView>(R.id.marker_text1)
    private val countView = findViewById<TextView>(R.id.marker_text2)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        val chart = chartView

        var thisWeekCount = 0L
        var prevWeekCount = 0L

        if (chart is LineChart) {
            val lineData = chart.lineData
            val dataSetList = lineData.dataSets // Get all the curves in the chart
            for (i in dataSetList.indices) {
                val dataSet = dataSetList[i] as LineDataSet
                // Get all the data sets on the Y axis of the curve, and
                // get the corresponding Y axis value according to the current X axis position
                val index = if (e!!.x.toInt() < dataSet.values.size) e.x.toInt() else 0
                val y = dataSet.values[index].y

                if (i == 0) {
                    thisWeekCount = y.toLong()
                    countView.text = context.getString(
                        R.string.stats_insights_views_and_visitors_tooltip_count,
                        y.toInt().toString(),
                        SelectedType.valueOf(selectedType).toString()
                    )
                }
                if (i == 1) {
                    prevWeekCount = y.toLong()
                }
            }
            val positive = thisWeekCount >= prevWeekCount
            val change = statsUtils.buildChange(prevWeekCount, thisWeekCount, positive, isFormattedNumber = false)
            changeView.text = change.toString()
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
        // posY posX refers to the position of the upper left corner of the markerView on the chart
        val width = width.toFloat()
        val height = height.toFloat()

        // If the y coordinate of the point is less than the height of the markerView,
        // if it is not processed, it will exceed the upper boundary. After processing,
        // the arrow is up at this time, and we need to move the icon down by the size of the arrow
        if (posY <= height + ARROW_SIZE) {
            offset.y = ARROW_SIZE
        } else {
            // Otherwise, it is normal, because our default is that the arrow is facing downwards,
            // and then the normal offset is that you need to offset the height of the markerView and the arrow size,
            // plus a stroke width, because you need to see the upper border of the dialog box
            offset.y = -height - ARROW_SIZE - STROKE_WIDTH
        }

        // handle X direction, left, middle, and right side of the chart
        if (posX > chartView.width - width) { // If it exceeds the right boundary, offset the view width to the left
            offset.x = -width
        } else { // by default, no offset (because the point is in the upper left corner)
            offset.x = 0F
            // If it is greater than half of the markerView, the arrow is in the middle,
            // so it is offset by half the width to the right
            if (posX > width / 2) {
                offset.x = -(width / 2)
            }
        }

        return offset
    }

    override fun draw(canvas: Canvas?, posX: Float, posY: Float) {
        super.draw(canvas, posX, posY)

        val saveId = canvas?.save()

        drawToolTip(canvas, posX, posY)
        draw(canvas)

        saveId?.let {
            canvas.restoreToCount(it)
        }
    }

    @Suppress("LongMethod")
    private fun drawToolTip(canvas: Canvas?, posX: Float, posY: Float) {
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = STROKE_WIDTH
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            pathEffect = CornerPathEffect(CORNER_RADIUS)
            color = context.getColor(R.color.blue_100)
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            strokeCap = Paint.Cap.ROUND
            pathEffect = CornerPathEffect(CORNER_RADIUS)
            color = context.getColor(R.color.blue_100)
        }

        val chart = chartView
        val width = width.toFloat()
        val height = height.toFloat()

        val offset = getOffsetForDrawingAtPoint(posX, posY)

        val path = Path()

        if (posY < height + ARROW_SIZE) { // Processing exceeds the upper boundary
            path.moveTo(0f, 0f)
            if (posX > chart.width - width) { // Exceed the right boundary
                path.lineTo(width - ARROW_SIZE, 0f)
                path.lineTo(width, -ARROW_SIZE + CIRCLE_OFFSET)
                path.lineTo(width, 0f)
            } else {
                if (posX > width / 2) { // In the middle of the chart
                    path.lineTo(width / 2 - ARROW_SIZE / 2, 0f)
                    path.lineTo(width / 2, -ARROW_SIZE + CIRCLE_OFFSET)
                    path.lineTo(width / 2 + ARROW_SIZE / 2, 0f)
                } else { // Exceed the left margin
                    path.lineTo(0f, -ARROW_SIZE + CIRCLE_OFFSET)
                    path.lineTo(0 + ARROW_SIZE, 0f)
                }
            }
            path.lineTo(0 + width, 0f)
            path.lineTo(0 + width, 0 + height)
            path.lineTo(0f, 0 + height)
            path.lineTo(0f, 0f)
            path.offset(posX + offset.x, posY + offset.y)
        } else { // Does not exceed the upper boundary
            path.moveTo(0f, 0f)
            path.lineTo(0 + width, 0f)
            path.lineTo(0 + width, 0 + height)
            if (posX > chart.width - width) {
                path.lineTo(width, height + ARROW_SIZE - CIRCLE_OFFSET)
                path.lineTo(width - ARROW_SIZE, 0 + height)
                path.lineTo(0f, 0 + height)
            } else {
                if (posX > width / 2) {
                    path.lineTo(width / 2 + ARROW_SIZE / 2, 0 + height)
                    path.lineTo(width / 2, height + ARROW_SIZE - CIRCLE_OFFSET)
                    path.lineTo(width / 2 - ARROW_SIZE / 2, 0 + height)
                    path.lineTo(0f, 0 + height)
                } else {
                    path.lineTo(0 + ARROW_SIZE, 0 + height)
                    path.lineTo(0f, height + ARROW_SIZE - CIRCLE_OFFSET)
                    path.lineTo(0f, 0 + height)
                }
            }
            path.lineTo(0f, 0f)
            path.offset(posX + offset.x, posY + offset.y)
        }
        path.close()

        // translate to the correct position and draw
        canvas?.apply {
            drawPath(path, bgPaint)
            drawPath(path, borderPaint)
            drawDataPoint(canvas, posX, posY)
            translate(posX + offset.x, posY + offset.y)
        }
    }

    private fun drawDataPoint(canvas: Canvas?, posX: Float, posY: Float) {
        val circleShadowPaint = Paint().apply {
            style = Paint.Style.FILL
            color = ContextCompat.getColor(context, R.color.gray_10)
            maskFilter = BlurMaskFilter(5F, NORMAL)
        }

        val circleBorderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = CIRCLE_STROKE_WIDTH
            isAntiAlias = true
            isDither = true
            color = ContextCompat.getColor(context, R.color.blue_0)
        }

        val circleFillPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            isDither = true
            color = ContextCompat.getColor(context, SelectedType.getColor(selectedType))
        }

        val circleShadowPath = Path().apply {
            addCircle(posX, posY, CIRCLE_SHADOW_RADIUS, CW)
        }

        val circleFillPath = Path().apply {
            addCircle(posX, posY, CIRCLE_RADIUS, CW)
        }

        val circleBorderPath = Path().apply {
            addCircle(posX, posY, CIRCLE_RADIUS, CW)
            fillType = Path.FillType.EVEN_ODD
        }

        val innerCircle = RectF().apply {
            inset(CIRCLE_STROKE_WIDTH, CIRCLE_STROKE_WIDTH)
        }
        if (innerCircle.width() > 0 && innerCircle.height() > 0) {
            circleBorderPath.addCircle(posX, posY, CIRCLE_RADIUS, CW)
        }

        canvas?.apply {
            drawPath(circleShadowPath, circleShadowPaint)
            drawPath(circleFillPath, circleFillPaint)
            drawPath(circleBorderPath, circleBorderPaint)
        }
    }

    companion object {
        const val CORNER_RADIUS = 10F
        const val ARROW_SIZE = 40F
        const val STROKE_WIDTH = 5F
        const val CIRCLE_OFFSET = 14F

        const val CIRCLE_RADIUS = 12F
        const val CIRCLE_SHADOW_RADIUS = CIRCLE_RADIUS + 2F
        const val CIRCLE_STROKE_WIDTH = 4F
    }
}
