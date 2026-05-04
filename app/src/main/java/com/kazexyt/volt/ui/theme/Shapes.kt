package com.kazexyt.volt.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

val VoltSquircle = RoundedPolygon(
    numVertices = 4,
    radius = 1f,
    rounding = CornerRounding(radius = 0.4f, smoothing = 0.8f)
)

class VoltSquircleShape(private val polygon: RoundedPolygon) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = polygon.toPath().asComposePath()
        val bounds = polygon.calculateBounds()

        // 1. Shift the path so its top-left corner starts exactly at (0,0)
        val translationMatrix = Matrix()
        translationMatrix.translate(-bounds[0], -bounds[1])
        path.transform(translationMatrix)

        // 2. Scale it to perfectly fill the button/mascot size
        val scaleMatrix = Matrix()
        scaleMatrix.scale(
            x = size.width / (bounds[2] - bounds[0]),
            y = size.height / (bounds[3] - bounds[1])
        )
        path.transform(scaleMatrix)

        return Outline.Generic(path)
    }
}
