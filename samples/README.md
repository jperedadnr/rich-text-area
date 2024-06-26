# The RichTextArea Samples

## BasicPromptDemo

The [BasicPromptDemo](/samples/src/main/java/com/gluonhq/richtextarea/samples/BasicPromptDemo.java) is the simplest use case of 
the RichTextArea control: it shows a prompt message, and the user can add text.

While all the control features are available, there are no menus or toolbars included, so user interaction is limited 
to shortcuts or the context menu.

For instance, after typing some text, select all (Ctrl/Cmd + A) or part of it (with mouse or keyboard) and press 
Ctrl/Cmd + I for italic or Ctrl/Cmd + B for bold.

Undo/Redo, Cut/Copy/Paste options work as usual. You can also copy text with emoji unicode, and paste it on the editor.
For instance, while running this sample, copy this text:

```
Hello Rich Text Area 👋🏼
```

and paste it inside the RichTextArea control, you should see the waving hand emoji and some text. Also copying from
the control and pasting it on the control itself or on any other application will work
too, keeping the rich content when possible.

Right click to display a context menu with different options, like inserting a
2x1 table.

To apply the rest of the control features, some UI is needed for user interaction. See
the [FullFeaturedDemo](#fullfeatureddemo) sample for a complete and advanced showcase.

### Usage

To run this sample, using Java 17+, do as follows:

```
./mvnw -fsamples javafx:run -Dmain.class=com.gluonhq.richtextarea.samples.BasicPromptDemo
```

![rta_editor.png](../.github/assets/BasicDemo.png)

## BasicDocumentDemo

The [BasicDocumentDemo](/samples/src/main/java/com/gluonhq/richtextarea/samples/BasicDocumentDemo.java) is an update
to the BasicPromptDemo where we add a decorated text to the RichTextArea control.

### Usage

To run this sample, using Java 17+, do as follows:

```
./mvnw -fsamples javafx:run -Dmain.class=com.gluonhq.richtextarea.samples.BasicDocumentDemo
```

## HighlightDemo

The [HighlightDemo](/samples/src/main/java/com/gluonhq/richtextarea/samples/HighlightDemo.java) shows how to use the 
RichTextArea control to add a document with some decorations, that are generated by searching some keywords over a 
given text.

### Usage

To run this sample, using Java 17+, do as follows:

```
./mvnw -fsamples javafx:run -Dmain.class=com.gluonhq.richtextarea.samples.HighlightDemo
```

![rta_editor.png](../.github/assets/HighlightDemo.png)

## ListsDemo

The [ListsDemo](/samples/src/main/java/com/gluonhq/richtextarea/samples/ListsDemo.java) shows a list, generated with 
custom numbered and bulleted decorations created via `RichTextArea::paragraphGraphicFactoryProperty`.

### Usage

To run this sample, using Java 17+, do as follows:

```
./mvnw -fsamples javafx:run -Dmain.class=com.gluonhq.richtextarea.samples.ListsDemo
```

![rta_editor.png](../.github/assets/ListsDemo.png)

## ActionsDemo

The [ActionsDemo](/samples/src/main/java/com/gluonhq/richtextarea/samples/ActionsDemo.java) shows how to use the 
RichTextArea control to render some text and interact with it in a basic way via three toggle buttons.

Run the sample and select some or all text, via mouse or keyboard, and then press the toggles to see how the decoration
of the selection changes accordingly.

Note that when you move the caret over the text, the toggles update their state 
(enabled means bold/italic/underline active), showing at any time the current decoration at the caret.

### Usage

To run this sample, using Java 17+, do as follows:

```
./mvnw -fsamples javafx:run -Dmain.class=com.gluonhq.richtextarea.samples.ActionsDemo
```

![rta_editor.png](../.github/assets/ActionsDemo.png)

## EmojiDemo

The [EmojiDemo](/samples/src/main/java/com/gluonhq/richtextarea/samples/EmojiDemo.java) shows how to use the 
RichTextArea control to render text and emojis.

This sample doesn't include a control to select interactively emojis (See [EmojiPopupDemo](#emojipopupdemo) for that).

### Usage

To run this sample, using Java 17+, do as follows:

```
./mvnw -fsamples javafx:run -Dmain.class=com.gluonhq.richtextarea.samples.EmojiDemo
```

![rta_editor.png](../.github/assets/EmojiDemo.png)

## EmojiPopupDemo

The [EmojiPopupDemo](/samples/src/main/java/com/gluonhq/richtextarea/samples/EmojiPopupDemo.java) shows how to use the
RichTextArea control to render text and emojis, including a popup control to select emojis interactively.

### Usage

To run this sample, using Java 17+, do as follows:

```
./mvnw -fsamples javafx:run -Dmain.class=com.gluonhq.richtextarea.samples.EmojiPopupDemo
```

![rta_editor.png](../.github/assets/EmojiPopupDemo.png)

## TableDemo

The [TableDemo](/samples/src/main/java/com/gluonhq/richtextarea/samples/TableDemo.java) shows how to use the
RichTextArea control to render a table, embedded into the text, including text and emojis.

### Usage

To run this sample, using Java 17+, do as follows:

```
./mvnw -fsamples javafx:run -Dmain.class=com.gluonhq.richtextarea.samples.TableDemo
```

![rta_editor.png](../.github/assets/TableDemo.png)

## FullFeaturedDemo

The [FullFeaturedDemo](/samples/src/main/java/com/gluonhq/richtextarea/samples/FullFeaturedDemo.java) shows a complete
use case of the RichTextArea control as a rich text editor. 

This is an advance sample that shows how to create a rich text editor, by using the RichTextArea control and adding
actions for the user interaction, via toolbars and menus, and most of the features of the control are showcased in this
sample.

### Usage

To run this sample, using Java 17+, do as follows:

```
./mvnw -fsamples javafx:run
```

![rta_editor.png](../.github/assets/rta_editor.png)
