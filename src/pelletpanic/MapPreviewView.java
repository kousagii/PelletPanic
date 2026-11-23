package pelletpanic;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;

/** Inline tutorial preview drawn from the same map data as the actual game. */
final class MapPreviewView extends View {
    private final Difficulty difficulty;

    MapPreviewView(Element element, Difficulty difficulty) {
        super(element);
        this.difficulty = difficulty;
    }

    @Override
    public float getPreferredSpan(int axis) {
        return axis == X_AXIS ? 600 : 395;
    }

    @Override
    public void paint(Graphics graphics, Shape allocation) {
        Rectangle bounds = allocation.getBounds();
        graphics.drawImage(GameRenderer.mapPreview(difficulty), bounds.x, bounds.y, bounds.width, bounds.height, null);
    }

    @Override
    public Shape modelToView(int position, Shape allocation, Position.Bias bias) throws BadLocationException {
        Rectangle bounds = allocation.getBounds();
        bounds.width = 0;
        return bounds;
    }

    @Override
    public int viewToModel(float x, float y, Shape allocation, Position.Bias[] bias) {
        bias[0] = Position.Bias.Forward;
        return getStartOffset();
    }
}
