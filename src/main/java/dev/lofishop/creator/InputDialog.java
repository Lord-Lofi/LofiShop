package dev.lofishop.creator;

import dev.lofishop.util.MessageUtil;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A pop-up window with one text box and Save / Cancel buttons, used by the
 * shop creator to ask for names, prices, amounts and limits.
 *
 * Replaces the old "type it in chat" prompts. The callbacks always run on the
 * main server thread.
 */
final class InputDialog {

    private static final String INPUT_KEY = "value";
    // The buttons stop working if the player leaves the window open longer than this.
    private static final Duration LIFETIME = Duration.ofMinutes(15);

    private InputDialog() {}

    /**
     * Shows the window.
     *
     * @param title    window title (MiniMessage)
     * @param hint     help text shown above the text box (MiniMessage)
     * @param error    error text from a previous attempt, or null (MiniMessage)
     * @param initial  text pre-filled in the box
     * @param onSave   receives the trimmed text when Save is clicked
     * @param onCancel runs when Cancel is clicked
     */
    static void show(Plugin plugin, Player player,
                     String title, String hint, String error, String initial,
                     Consumer<String> onSave, Runnable onCancel) {

        List<DialogBody> body = new ArrayList<>();
        if (error != null) body.add(DialogBody.plainMessage(MessageUtil.parse(error)));
        body.add(DialogBody.plainMessage(MessageUtil.parse(hint)));

        ClickCallback.Options once = ClickCallback.Options.builder()
                .uses(1)
                .lifetime(LIFETIME)
                .build();

        ActionButton save = ActionButton.builder(MessageUtil.parse("<green>Save"))
                .action(DialogAction.customClick((response, audience) -> {
                    String text = response.getText(INPUT_KEY);
                    String value = text == null ? "" : text.trim();
                    runOnMain(plugin, player, () -> onSave.accept(value));
                }, once))
                .build();

        ActionButton cancel = ActionButton.builder(MessageUtil.parse("<red>Cancel"))
                .action(DialogAction.customClick(
                        (response, audience) -> runOnMain(plugin, player, onCancel), once))
                .build();

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(MessageUtil.parse(title))
                        // Escape would close the window without either callback and strand the
                        // player outside the editor, so they must choose Save or Cancel.
                        .canCloseWithEscape(false)
                        .body(body)
                        .inputs(List.of(DialogInput.text(INPUT_KEY, Component.empty())
                                .labelVisible(false)
                                .initial(initial == null ? "" : initial)
                                .maxLength(256)
                                .width(300)
                                .build()))
                        .build())
                .type(DialogType.confirmation(save, cancel)));

        player.showDialog(dialog);
    }

    private static void runOnMain(Plugin plugin, Player player, Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) task.run();
        });
    }
}
