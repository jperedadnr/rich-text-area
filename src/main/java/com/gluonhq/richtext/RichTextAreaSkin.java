package com.gluonhq.richtext;

import com.gluonhq.richtext.model.PieceTable;
import com.gluonhq.richtext.model.TextBuffer;
import com.gluonhq.richtext.model.TextDecoration;
import com.gluonhq.richtext.viewmodel.ActionCmd;
import com.gluonhq.richtext.viewmodel.ActionCmdFactory;
import com.gluonhq.richtext.viewmodel.RichTextAreaViewModel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SkinBase;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.LineTo;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.gluonhq.richtext.viewmodel.RichTextAreaViewModel.Direction;
import static java.util.Map.entry;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static javafx.scene.text.FontPosture.ITALIC;
import static javafx.scene.text.FontWeight.BOLD;

public class RichTextAreaSkin extends SkinBase<RichTextArea> {

    interface ActionBuilder extends Function<KeyEvent, ActionCmd>{}

    private static final ActionCmdFactory ACTION_CMD_FACTORY = new ActionCmdFactory();

    private static final Map<KeyCombination, ActionBuilder> INPUT_MAP = Map.ofEntries(
        entry( new KeyCodeCombination(RIGHT, SHIFT_ANY, ALT_ANY, CONTROL_ANY, SHORTCUT_ANY), e -> ACTION_CMD_FACTORY.caretMove(Direction.FORWARD, e)),
        entry( new KeyCodeCombination(LEFT,  SHIFT_ANY, ALT_ANY, CONTROL_ANY, SHORTCUT_ANY), e -> ACTION_CMD_FACTORY.caretMove(Direction.BACK, e)),
        entry( new KeyCodeCombination(DOWN,  SHIFT_ANY),                                     e -> ACTION_CMD_FACTORY.caretMove(Direction.DOWN, e.isShiftDown(), false, false)),
        entry( new KeyCodeCombination(UP,    SHIFT_ANY),                                     e -> ACTION_CMD_FACTORY.caretMove(Direction.UP, e.isShiftDown(), false, false)),
        entry( new KeyCodeCombination(HOME,  SHIFT_ANY),                                     e -> ACTION_CMD_FACTORY.caretMove(Direction.FORWARD, e.isShiftDown(), false, true)),
        entry( new KeyCodeCombination(END,   SHIFT_ANY),                                     e -> ACTION_CMD_FACTORY.caretMove(Direction.BACK, e.isShiftDown(), false, true)),
        entry( new KeyCodeCombination(C, SHORTCUT_DOWN),                                     e -> ACTION_CMD_FACTORY.copy()),
        entry( new KeyCodeCombination(X, SHORTCUT_DOWN),                                     e -> ACTION_CMD_FACTORY.cut()),
        entry( new KeyCodeCombination(V, SHORTCUT_DOWN),                                     e -> ACTION_CMD_FACTORY.paste()),
        entry( new KeyCodeCombination(Z, SHORTCUT_DOWN),                                     e -> ACTION_CMD_FACTORY.undo()),
        entry( new KeyCodeCombination(Z, SHORTCUT_DOWN, SHIFT_DOWN),                         e -> ACTION_CMD_FACTORY.redo()),
        entry( new KeyCodeCombination(ENTER, SHIFT_ANY),                                     e -> ACTION_CMD_FACTORY.insertText("\n")),
        entry( new KeyCodeCombination(BACK_SPACE, SHIFT_ANY),                                e -> ACTION_CMD_FACTORY.removeText(-1)),
        entry( new KeyCodeCombination(DELETE),                                               e -> ACTION_CMD_FACTORY.removeText(0)),
        entry( new KeyCodeCombination(B, SHORTCUT_DOWN),                                     e -> ACTION_CMD_FACTORY.decorateText(TextDecoration.builder().fontWeight(BOLD).build())),
        entry( new KeyCodeCombination(I, SHORTCUT_DOWN),                                     e -> ACTION_CMD_FACTORY.decorateText(TextDecoration.builder().fontPosture(ITALIC).build()))
    );

    // TODO need to find a better way to find next row caret position
    private final RichTextAreaViewModel viewModel = new RichTextAreaViewModel(this::getNextRowPosition);

    private final ScrollPane scrollPane;
    private final TextFlow textFlow = new TextFlow();
    private final Path caretShape = new Path();
    private final Path selectionShape = new Path();
    private final Group layers;
    private final ObservableSet<Path> textBackgroundColorPaths = FXCollections.observableSet();

    private final Timeline caretTimeline = new Timeline(
        new KeyFrame(Duration.ZERO        , e -> setCaretVisibility(false)),
        new KeyFrame(Duration.seconds(0.5), e -> setCaretVisibility(true)),
        new KeyFrame(Duration.seconds(1.0))
    );

    private final Map<Integer, Font> fontCache = new ConcurrentHashMap<>();
    private final SmartTimer fontCacheEvictionTimer = new SmartTimer( this::evictUnusedFonts, 1000, 60000);

    private final Consumer<TextBuffer.Event> textChangeListener = e -> refreshTextFlow();
    private final ChangeListener<Boolean> focusChangeListener;
    private final SetChangeListener<Path> textBackgroundColorPathsChangeListener = this::updateLayers;

    //TODO remove listener on viewModel change
    private final ChangeListener<Number> caretPositionListener = (o, ocp, p) -> {
        int caretPosition = p.intValue();
        caretShape.getElements().clear();
        if (caretPosition < 0) {
            caretTimeline.stop();
        } else {
            var pathElements = textFlow.caretShape(caretPosition, true);
            caretShape.getElements().addAll(pathElements);
            if (caretShape.getLayoutBounds().getHeight() < 3) {
                caretShape.getElements().add(new LineTo(0, 20));
            }
            caretTimeline.play();
        }
    };

    //TODO remove listener on viewModel change
    private final ChangeListener<Selection> selectionListener = (o, os, selection) -> {
        selectionShape.getElements().clear();
        if (selection.isDefined()) {
            selectionShape.getElements().setAll(textFlow.rangeShape(selection.getStart(), selection.getEnd()));
        }
    };

    protected RichTextAreaSkin(final RichTextArea control) {
        super(control);

        textFlow.setFocusTraversable(false);
        textFlow.setPadding(new Insets(-1));
        textFlow.getStyleClass().setAll("text-flow");

        caretShape.setFocusTraversable(false);
        caretShape.getStyleClass().add("caret");

        selectionShape.getStyleClass().setAll("selection");

        layers = new Group(selectionShape, caretShape, textFlow);
        layers.getChildren().addAll(0, textBackgroundColorPaths);
        caretTimeline.setCycleCount(Timeline.INDEFINITE);

        scrollPane = new ScrollPane(layers);
        scrollPane.setFocusTraversable(false);
        focusChangeListener = (obs, ov, nv) -> {
            if (nv) {
                getSkinnable().requestFocus();
            }
        };
        scrollPane.focusedProperty().addListener(focusChangeListener);
        getChildren().add(scrollPane);

        // all listeners have to be removed within dispose method
        control.faceModelProperty().addListener((obs, ov, nv) -> {
            if (ov != null) {
                dispose();
            }
            setup(nv);
        });
        setup(control.getFaceModel());
    }

    private void setup(FaceModel faceModel) {
        if (faceModel == null) {
            return;
        }
        viewModel.setTextBuffer(new PieceTable(faceModel));
        getSkinnable().textLengthProperty.bind(viewModel.textLengthProperty());
        viewModel.caretPositionProperty().addListener(caretPositionListener);
        viewModel.selectionProperty().addListener(selectionListener);
        viewModel.addChangeListener(textChangeListener);
        getSkinnable().editableProperty().addListener(this::editableChangeListener);
        editableChangeListener(null); // sets up all related listeners
        textBackgroundColorPaths.addListener(textBackgroundColorPathsChangeListener);
        refreshTextFlow();
        viewModel.setCaretPosition(faceModel.getCaretPosition());
    }
    /// PROPERTIES ///////////////////////////////////////////////////////////////


    /// PUBLIC METHODS  /////////////////////////////////////////////////////////

    @Override
    public void dispose() {
        getSkinnable().editableProperty().removeListener(this::editableChangeListener);
        getSkinnable().textLengthProperty.unbind();
        viewModel.clearSelection();
        viewModel.caretPositionProperty().removeListener(caretPositionListener);
        viewModel.selectionProperty().removeListener(selectionListener);
        viewModel.removeChangeListener(textChangeListener);
        textBackgroundColorPaths.removeListener(textBackgroundColorPathsChangeListener);
    }

    public RichTextAreaViewModel getViewModel() {
        return viewModel;
    }

    /// PRIVATE METHODS /////////////////////////////////////////////////////////

    // TODO Need more optimal way of rendering text fragments.
    //  For now rebuilding the whole text flow
    private void refreshTextFlow() {
        fontCacheEvictionTimer.pause();
        try {
            var fragments = new ArrayList<Text>();
            var backgroundIndexRanges = new ArrayList<IndexRangeColor>();
            var length = new AtomicInteger();
            viewModel.walkFragments((text, decoration) -> {
                final Text textNode = buildText(text, decoration);
                fragments.add(textNode);

                if (decoration.getBackground() != Color.TRANSPARENT) {
                    final IndexRangeColor indexRangeColor = new IndexRangeColor(
                            length.get(),
                            length.get() + textNode.getText().length(),
                            decoration.getBackground()
                    );
                    backgroundIndexRanges.add(indexRangeColor);
                }
                length.addAndGet(textNode.getText().length());
            });
            textFlow.getChildren().setAll(fragments);
            addBackgroundPathsToLayers(backgroundIndexRanges);
        } finally {
            fontCacheEvictionTimer.start();
        }
    }

    private void addBackgroundPathsToLayers(List<IndexRangeColor> backgroundIndexRanges) {
        final List<Path> backgroundPaths = backgroundIndexRanges.stream()
                .map(indexRangeBackground -> {
                    final Path path = new BackgroundColorPath(textFlow.rangeShape(indexRangeBackground.getStart(), indexRangeBackground.getEnd()));
                    path.setStrokeWidth(0);
                    path.setFill(indexRangeBackground.getColor());
                    return path;
                })
                .collect(Collectors.toList());
        textBackgroundColorPaths.removeIf(path -> !backgroundPaths.contains(path));
        textBackgroundColorPaths.addAll(backgroundPaths);
    }

    private Text buildText(String content, TextDecoration decoration ) {
        Objects.requireNonNull(decoration);
        Text text = new Text(Objects.requireNonNull(content));
        text.setFill(decoration.getForeground());

        // Cashing fonts, assuming their reuse, especially for default one
        int hash = Objects.hash(
                decoration.getFontFamily(),
                decoration.getFontWeight(),
                decoration.getFontPosture(),
                decoration.getFontSize());

        Font font = fontCache.computeIfAbsent( hash,
            h -> Font.font(
                    decoration.getFontFamily(),
                    decoration.getFontWeight(),
                    decoration.getFontPosture(),
                    decoration.getFontSize()));

        text.setFont(font);
        return text;
    }

    private void evictUnusedFonts() {
        Set<Font> usedFonts =  textFlow.getChildren()
                .stream()
                .filter(Text.class::isInstance)
                .map( t -> ((Text)t).getFont())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Font> cachedFonts = new ArrayList<>(fontCache.values());
        cachedFonts.removeAll(usedFonts);
        fontCache.values().removeAll(cachedFonts);
    }

    private void editableChangeListener(Observable o) {
        boolean editable = getSkinnable().isEditable();

        viewModel.clearSelection();
        if (editable) {
            getSkinnable().setOnKeyPressed(this::keyPressedListener);
            getSkinnable().setOnKeyTyped(this::keyTypedListener);
            textFlow.setOnMousePressed(this::mousePressedListener);
            textFlow.setOnMouseDragged(this::mouseDraggedListener);
        } else {
            getSkinnable().setOnKeyPressed(null);
            getSkinnable().setOnKeyTyped(null);
            scrollPane.focusedProperty().removeListener(focusChangeListener);
            textFlow.setOnMousePressed(null);
            textFlow.setOnMouseDragged(null);
        }

        viewModel.setCaretPosition( editable? 0:-1 );
        textFlow.setCursor( editable? Cursor.TEXT: Cursor.DEFAULT);

    }

    private void updateLayers(SetChangeListener.Change<? extends Path> change) {
        if (change.wasAdded()) {
            layers.getChildren().add(0, change.getElementAdded());
        } else if (change.wasRemoved()) {
            layers.getChildren().remove(change.getElementRemoved());
        }
    }

    private void setCaretVisibility(boolean on) {
        if (caretShape.getElements().size() > 0) {
            // Opacity is used since we don't want the changing caret bounds to affect the layout
            // Otherwise text appears to be jumping
            caretShape.setOpacity( on? 1: 0 );
        }
    }

    private int dragStart = -1;

    private void mousePressedListener(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY && !(e.isMiddleButtonDown() || e.isSecondaryButtonDown())) {
            HitInfo hitInfo = textFlow.hitTest(new Point2D(e.getX(), e.getY()));
            int prevCaretPosition = viewModel.getCaretPosition();
            if (hitInfo.getInsertionIndex() >= 0) {
                if (!(e.isControlDown() || e.isAltDown() || e.isShiftDown() || e.isMetaDown() || e.isShortcutDown())) {
                    viewModel.setCaretPosition(hitInfo.getInsertionIndex());
                    if (e.getClickCount() == 2) {
                        viewModel.selectCurrentWord();
                    } else if (e.getClickCount() == 3) {
                        viewModel.selectCurrentLine();
                    } else {
                        dragStart = prevCaretPosition;
                        viewModel.clearSelection();
                    }
                } else if (e.isShiftDown() && e.getClickCount() == 1 && !(e.isControlDown() || e.isAltDown() || e.isMetaDown() || e.isShortcutDown())) {
                    viewModel.setSelection(new Selection(prevCaretPosition, hitInfo.getInsertionIndex()));
                    viewModel.setCaretPosition(hitInfo.getInsertionIndex());
                }
            }
            getSkinnable().requestFocus();
            e.consume();
        } else {
            // TODO Add support for ContextMenu
        }
    }

    private void mouseDraggedListener(MouseEvent e) {
        HitInfo hitInfo = textFlow.hitTest(new Point2D(e.getX(), e.getY()));
        if (hitInfo.getInsertionIndex() >= 0) {
            int dragEnd = hitInfo.getInsertionIndex();
            viewModel.setSelection( new Selection(dragStart, dragEnd));
            viewModel.setCaretPosition(hitInfo.getInsertionIndex());
        }
        e.consume();
    }

    // So far the only way to find prev/next row location is to use the size of the caret,
    // which always has the height of the row. Adding line spacing to it allows us to find a point which
    // belongs to the desired row. Then using the `hitTest` we can find the related caret position.
    private int getNextRowPosition(double x, boolean down) {
        Bounds caretBounds = caretShape.getLayoutBounds();
        double nextRowPos = x < 0d ?
                down ?
                    caretBounds.getMaxY() + textFlow.getLineSpacing() :
                    caretBounds.getMinY() - textFlow.getLineSpacing() :
                caretBounds.getCenterY();
        double xPos = x < 0d ? caretBounds.getMaxX() : x;
        HitInfo hitInfo = textFlow.hitTest(new Point2D(xPos, nextRowPos));
        return hitInfo.getInsertionIndex();
    }

    private static boolean isPrintableChar(char c) {
        Character.UnicodeBlock changeBlock = Character.UnicodeBlock.of(c);
        return (c == '\n' || c == '\t' || !Character.isISOControl(c)) &&
                !KeyEvent.CHAR_UNDEFINED.equals(String.valueOf(c)) &&
                changeBlock != null && changeBlock != Character.UnicodeBlock.SPECIALS;
    }

    private static boolean isCharOnly(KeyEvent e) {
        char c = e.getCharacter().isEmpty()? 0: e.getCharacter().charAt(0);
        return isPrintableChar(c) &&
               !e.isControlDown() &&
               !e.isMetaDown() &&
               !e.isAltDown();
    }

    private void execute(ActionCmd action) {
        Objects.requireNonNull(action).apply(viewModel);
    }

    private void keyPressedListener(KeyEvent e) {

        // Find an applicable action and execute it if found
        for (KeyCombination kc : INPUT_MAP.keySet()) {
            if (kc.match(e)) {
                execute(INPUT_MAP.get(kc).apply(e));
                e.consume();
                return;
            }
        }

    }

    private void keyTypedListener(KeyEvent e) {
        if (isCharOnly(e)) {
            execute(ACTION_CMD_FACTORY.insertText(e.getCharacter()));
            e.consume();
        }
    }

    private static class IndexRangeColor {

        private final int start;
        private final int end;
        private final Color color;

        public IndexRangeColor(int start, int end, Color color) {
            this.start = start;
            this.end = end;
            this.color = color;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public Color getColor() {
            return color;
        }
    }

    private static class BackgroundColorPath extends Path {

        public BackgroundColorPath(PathElement[] elements) {
            super(elements);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BackgroundColorPath that = (BackgroundColorPath) o;
            return Objects.equals(getLayoutBounds(), that.getLayoutBounds()) && Objects.equals(getFill(), that.getFill());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getLayoutBounds(), getFill());
        }
    }

}