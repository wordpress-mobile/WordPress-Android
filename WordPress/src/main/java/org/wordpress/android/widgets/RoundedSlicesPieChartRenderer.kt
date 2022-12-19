package org.wordpress.android.widgets

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Path.Direction.CCW
import android.graphics.Path.Direction.CW
import android.graphics.RectF
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.interfaces.datasets.IPieDataSet
import com.github.mikephil.charting.renderer.PieChartRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Utils
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * PieChartRenderer draws outer round to the end of the slice and inner round to the start of the slice.
 * PieChartRenderer#drawDataSet is copied here and edited for a feature which outer round can be drew both start and end
 * of slices.
 */
class RoundedSlicesPieChartRenderer(chart: PieChart) : PieChartRenderer(chart, chart.animator, chart.viewPortHandler) {
    init {
        chart.setDrawRoundedSlices(true)
        chart.isDrawHoleEnabled = false
        chart.setTouchEnabled(false)
    }

    // These are suppressed instead of fixing for keeping the similarity with super class function
    @Suppress("LongMethod", "ComplexMethod", "NestedBlockDepth", "MagicNumber")
    override fun drawDataSet(c: Canvas, dataSet: IPieDataSet) {
        var angle = 0f
        val rotationAngle = mChart.rotationAngle

        val phaseX = mAnimator.phaseX
        val phaseY = mAnimator.phaseY

        val circleBox = mChart.circleBox

        val entryCount = dataSet.entryCount
        val drawAngles = mChart.drawAngles
        val center = mChart.centerCircleBox
        val radius = mChart.radius
        val userInnerRadius = radius * (mChart.holeRadius / 100f)
        val roundedRadius = (radius - radius * mChart.holeRadius / 100f) / 2f
        val roundedCircleBox = RectF()

        var visibleAngleCount = 0
        for (j in 0 until entryCount) {
            // draw only if the value is greater than zero
            if (abs(dataSet.getEntryForIndex(j).y) > Utils.FLOAT_EPSILON) {
                visibleAngleCount++
            }
        }

        val sliceSpace = if (visibleAngleCount <= 1) 0f else getSliceSpace(dataSet)
        val pathBuffer = Path()
        val innerRectBuffer = RectF()

        for (j in 0 until entryCount) {
            val sliceAngle = drawAngles[j]
            var innerRadius = userInnerRadius
            val e = dataSet.getEntryForIndex(j)

            // draw only if the value is greater than zero
            if (abs(e.y) <= Utils.FLOAT_EPSILON) {
                angle += sliceAngle * phaseX
                continue
            }

            val accountForSliceSpacing = sliceSpace > 0f && sliceAngle <= 180f
            mRenderPaint.color = dataSet.getColor(j)
            val sliceSpaceAngleOuter = if (visibleAngleCount == 1) {
                0f
            } else {
                sliceSpace / (Utils.FDEG2RAD * radius)
            }
            val startAngleOuter = rotationAngle + (angle + sliceSpaceAngleOuter / 2f) * phaseY
            var sweepAngleOuter = (sliceAngle - sliceSpaceAngleOuter) * phaseY
            if (sweepAngleOuter < 0f) {
                sweepAngleOuter = 0f
            }
            pathBuffer.reset()

            var x = center.x + (radius - roundedRadius) * cos(startAngleOuter * Utils.FDEG2RAD)
            var y = center.y + (radius - roundedRadius) * sin(startAngleOuter * Utils.FDEG2RAD)
            roundedCircleBox[x - roundedRadius, y - roundedRadius, x + roundedRadius] = y + roundedRadius

            val arcStartPointX = center.x + radius * cos(startAngleOuter * Utils.FDEG2RAD)
            val arcStartPointY = center.y + radius * sin(startAngleOuter * Utils.FDEG2RAD)
            if (sweepAngleOuter >= 360f && sweepAngleOuter % 360f <= Utils.FLOAT_EPSILON) {
                // Android is doing "mod 360"
                pathBuffer.addCircle(center.x, center.y, radius, CW)
            } else {
                pathBuffer.arcTo(roundedCircleBox, startAngleOuter - 180, 180f)
                pathBuffer.arcTo(circleBox, startAngleOuter, sweepAngleOuter)
            }

            // API < 21 does not receive floats in addArc, but a RectF
            innerRectBuffer[center.x - innerRadius, center.y - innerRadius, center.x + innerRadius] =
                    center.y + innerRadius
            if (innerRadius > 0f || accountForSliceSpacing) {
                if (accountForSliceSpacing) {
                    var minSpacedRadius = calculateMinimumRadiusForSpacedSlice(
                            center,
                            radius,
                            sliceAngle * phaseY,
                            arcStartPointX, arcStartPointY,
                            startAngleOuter,
                            sweepAngleOuter
                    )
                    if (minSpacedRadius < 0f) {
                        minSpacedRadius = -minSpacedRadius
                    }
                    innerRadius = max(innerRadius, minSpacedRadius)
                }
                val sliceSpaceAngleInner = if (visibleAngleCount == 1 || innerRadius == 0f) {
                    0f
                } else {
                    sliceSpace / (Utils.FDEG2RAD * innerRadius)
                }
                val startAngleInner = rotationAngle + (angle + sliceSpaceAngleInner / 2f) * phaseY
                var sweepAngleInner = (sliceAngle - sliceSpaceAngleInner) * phaseY
                if (sweepAngleInner < 0f) {
                    sweepAngleInner = 0f
                }
                val endAngleInner = startAngleInner + sweepAngleInner
                if (sweepAngleOuter >= 360f && sweepAngleOuter % 360f <= Utils.FLOAT_EPSILON) {
                    // Android is doing "mod 360"
                    pathBuffer.addCircle(center.x, center.y, innerRadius, CCW)
                } else {
                    x = center.x + (radius - roundedRadius) * cos(endAngleInner * Utils.FDEG2RAD)
                    y = center.y + (radius - roundedRadius) * sin(endAngleInner * Utils.FDEG2RAD)
                    roundedCircleBox[x - roundedRadius, y - roundedRadius, x + roundedRadius] = y + roundedRadius
                    pathBuffer.arcTo(roundedCircleBox, endAngleInner, 180f)
                    pathBuffer.arcTo(innerRectBuffer, endAngleInner, -sweepAngleInner)
                }
            } else {
                if (sweepAngleOuter % 360f > Utils.FLOAT_EPSILON) {
                    if (accountForSliceSpacing) {
                        val angleMiddle = startAngleOuter + sweepAngleOuter / 2f
                        val sliceSpaceOffset = calculateMinimumRadiusForSpacedSlice(
                                center,
                                radius,
                                sliceAngle * phaseY,
                                arcStartPointX,
                                arcStartPointY,
                                startAngleOuter,
                                sweepAngleOuter
                        )
                        val arcEndPointX = center.x + sliceSpaceOffset * cos(angleMiddle * Utils.FDEG2RAD)
                        val arcEndPointY = center.y + sliceSpaceOffset * sin(angleMiddle * Utils.FDEG2RAD)
                        pathBuffer.lineTo(arcEndPointX, arcEndPointY)
                    } else {
                        pathBuffer.lineTo(center.x, center.y)
                    }
                }
            }
            pathBuffer.close()
            mBitmapCanvas.drawPath(pathBuffer, mRenderPaint)
            angle += sliceAngle * phaseX
        }

        MPPointF.recycleInstance(center)
    }
}
