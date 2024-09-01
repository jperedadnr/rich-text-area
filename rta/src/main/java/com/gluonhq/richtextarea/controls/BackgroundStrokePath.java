/*
 * Copyright (c) 2022, 2024, Gluon
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

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Shape;

import java.util.Objects;

public class BackgroundStrokePath extends Path implements BackgroundPath {

    private final Paint color;

    public BackgroundStrokePath(PathElement[] elements, Paint color) {
        super(elements);
        this.color = color;
        setFill(null);
        setStrokeWidth(1);
        setStroke(color);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BackgroundStrokePath that = (BackgroundStrokePath) o;
        return Objects.equals(getLayoutBounds(), that.getLayoutBounds()) && Objects.equals(getStroke(), that.getStroke());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLayoutBounds(), getStroke());
    }

    @Override
    public BackgroundPath mergeWith(Path other) {
        if (other instanceof BackgroundStrokePath) {
            Path union = (Path) Shape.union(this, other);
            union.setFill(Color.BLACK);
            union.setStrokeWidth(1);
            union.setStroke(this.getStroke());
            return new BackgroundStrokePath(union.getElements().toArray(new PathElement[0]), color);
        }
        return this;
    }

    @Override
    public Paint getKey() {
        return getStroke();
    }
}
