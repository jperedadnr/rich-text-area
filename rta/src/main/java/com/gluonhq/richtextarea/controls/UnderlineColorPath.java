/*
 * Copyright (c) 2024, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.richtextarea.controls;

import com.gluonhq.richtextarea.model.TextDecoration;
import javafx.collections.ObservableList;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A linear path with that is generated from
 * the {@link Shape} of a {@link javafx.scene.text.Text} node inside a
 * {@link javafx.scene.text.TextFlow}, that can span one or more lines.
 */
public class UnderlineColorPath extends Path implements BackgroundPath {

    private final double baseline;
    private final Paint color;
    private final TextDecoration.BackgroundType backgroundType;

    /**
     * Creates a UnderlineColorPath
     *
     * @param elements an array of {@link PathElement pathElements} that define
     *                 the {@link Shape} of a {@link javafx.scene.text.Text} node
     * @param baseline the baseline offset
     */
    public UnderlineColorPath(PathElement[] elements, double baseline, Paint color, TextDecoration.BackgroundType backgroundType) {
        this.baseline = baseline;
        this.color = color;
        this.backgroundType = backgroundType;
        Path refPath = new Path(elements);
        final double pathMinX = refPath.getLayoutBounds().getMinX();
        final double pathWidth = refPath.getLayoutBounds().getWidth();
        List<LineTo> lines = refPath.getElements().stream()
                .filter(LineTo.class::isInstance)
                .map(LineTo.class::cast)
                .collect(Collectors.toList());
        // Get list of yMin per line, and yMax
        List<Double> yList = lines.stream()
                        .map(LineTo::getY)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

        for (int i = 0; i < yList.size() - 1; i++) {
            double lineY = yList.get(i);
            // Determine horizontal range of underline path:
            Path tmpPath = (Path) Shape.intersect(refPath, new Rectangle(pathMinX, lineY + 1, pathWidth, 1));
            double xMin = tmpPath.getLayoutBounds().getMinX() + 0.5;
            double xMax = tmpPath.getLayoutBounds().getMaxX() - 0.5;
            if (backgroundType == TextDecoration.BackgroundType.WAVY) {
                createWavyPath(getElements(), xMin, xMax, lineY + baseline);
            } else if (backgroundType == TextDecoration.BackgroundType.UNDERLINE) {
                getElements().add(new MoveTo(xMin, lineY + baseline + 1));
                getElements().add(new LineTo(xMax, lineY + baseline + 1));
            }
        }

        setFill(null);
        setStroke(color);
        setStrokeWidth(1);
        getStyleClass().add("path");
        getStyleClass().add(backgroundType.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnderlineColorPath that = (UnderlineColorPath) o;
        return Objects.equals(getLayoutBounds(), that.getLayoutBounds()) && Objects.equals(getStroke(), that.getStroke());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLayoutBounds(), getStroke());
    }

    @Override
    public BackgroundPath mergeWith(Path other) {
        if (other instanceof UnderlineColorPath) {
            Path union = (Path) Shape.union(this, other);
            union.setFill(null);
            union.setStrokeWidth(1);
            union.setStroke(this.getStroke());
            return new UnderlineColorPath(union.getElements().toArray(new PathElement[0]), baseline, color, backgroundType);
        }
        return null;
    }

    @Override
    public Paint getKey() {
        return getStroke();
    }

    private void createWavyPath(ObservableList<PathElement> elements, double xMin, double xMax, double y) {
        int squiggle = 2;
        double x = xMin;
        double r = squiggle / 2d;
        y += r;
        elements.add(new MoveTo(x, y));
        outerLoop:
        while (true) {
            for (int j = 0; j < 2; j++) {
                double newX = Math.min(x + squiggle, xMax);
                double newY = Math.sqrt(r * r - (newX - x - r) * (newX - x - r)) + y;
                double ang = 180 - Math.toDegrees(Math.atan2(newY - y, newX - x - r));
                elements.add(new ArcTo(r, r, ang, newX, newY, ang > 90, j == 1));
                if (newX >= xMax) {
                    break outerLoop;
                }
                x += squiggle;
            }
        }
    }
}
