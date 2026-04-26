package dev.tamago0314.hikariTweaks.gui;

import dev.tamago0314.hikariTweaks.HikariTweaksClient;
import dev.tamago0314.hikariTweaks.config.ClientConfigManager;
import dev.tamago0314.hikariTweaks.config.TweaksOptions;
import fi.dy.masa.malilib.config.options.BooleanHotkeyGuiWrapper;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

import java.util.Collections;
import java.util.List;

/**
 * Hikari-Tweaks のメイン設定画面。
 *
 * ScoreboardTab.WidgetHost を実装し、ScoreboardTab が生成した
 * ButtonGeneric / WidgetSlider をこの GUI の malilib リストに登録する。
 * これにより malilib の onMouseClicked ループのみがクリック音を鳴らし、
 * バニラ Screen.mouseClicked() による二重再生を防ぐ。
 */
public class HikariTweaksConfigScreen extends GuiConfigsBase
        implements ScoreboardTab.WidgetHost {

    private static ConfigGuiTab tab = ConfigGuiTab.TWEAKS;
    private ScoreboardTab scoreboardTab;

    public HikariTweaksConfigScreen(Screen parent) {
        super(10, 50, "hikari-tweaks", parent,
                HikariTweaksClient.MOD_NAME + " " + HikariTweaksClient.MOD_VERSION);
        this.setParent(parent);
    }

    // ── WidgetHost ──────────────────────────────────────────────────────────────

    @Override
    public <T extends ButtonBase> T addButton(T button, IButtonActionListener listener) {
        return super.addButton(button, listener);
    }

    @Override
    public <T extends WidgetBase> T addWidget(T widget) {
        return super.addWidget(widget);
    }

    // ── 初期化 ──────────────────────────────────────────────────────────────────

    @Override
    public void initGui() {
        if (scoreboardTab != null) {
            scoreboardTab.onClose();
            scoreboardTab = null;
        }
        super.initGui();
        this.clearOptions();

        int x = 10, y = 26;
        for (ConfigGuiTab t : ConfigGuiTab.values()) {
            x += createTabButton(x, y, -1, t);
        }

        if (tab == ConfigGuiTab.SCOREBOARD) {
            scoreboardTab = new ScoreboardTab(this, this);
            int contentY = 50;
            scoreboardTab.init(10, contentY, this.width - 20, this.height - contentY - 4);
        }
    }

    private int createTabButton(int x, int y, int width, ConfigGuiTab targetTab) {
        ButtonGeneric btn = new ButtonGeneric(x, y, width, 20, targetTab.getDisplayName());
        btn.setEnabled(tab != targetTab);
        this.addButton(btn, new TabButtonListener(targetTab, this));
        return btn.getWidth() + 2;
    }

    // ── config ──────────────────────────────────────────────────────────────────

    @Override protected int getConfigWidth() {
        return switch (tab) {
            case LISTS      -> 220;
            case HOTKEYS    -> 240;
            case SCOREBOARD -> 0;
            default         -> 260;
        };
    }

    @Override protected int getBrowserWidth()  {
        return tab == ConfigGuiTab.SCOREBOARD ? 0 : super.getBrowserWidth();
    }
    @Override protected int getBrowserHeight() {
        return tab == ConfigGuiTab.SCOREBOARD ? 0 : super.getBrowserHeight();
    }
    @Override protected boolean useKeybindSearch() {
        return tab == ConfigGuiTab.TWEAKS || tab == ConfigGuiTab.HOTKEYS;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return switch (tab) {
            case TWEAKS -> ConfigOptionWrapper.createFor(List.of(
                    wrapConfig(TweaksOptions.FIX_BEACON_RANGE_FREE_CAM),
                    wrapConfig(TweaksOptions.DURABILITY_WARNING_ENABLED),
                    wrapConfig(TweaksOptions.AUTO_RESTOCK_HOTBAR),
                    wrapConfig(TweaksOptions.TOTEM_RESTOCK)
            ));
            case LISTS      -> ConfigOptionWrapper.createFor(TweaksOptions.lists());
            case HOTKEYS    -> ConfigOptionWrapper.createFor(TweaksOptions.hotkeys());
            case SCOREBOARD -> Collections.emptyList();
        };
    }

    // ── 描画 ────────────────────────────────────────────────────────────────────

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        if (tab == ConfigGuiTab.SCOREBOARD && scoreboardTab != null) {
            scoreboardTab.render(matrices, mouseX, mouseY, delta);
        }
    }

    // ── 入力処理 ────────────────────────────────────────────────────────────────

    /**
     * mouseClicked をオーバーライドして「malilib の onMouseClicked のみ呼ぶ」ようにする。
     *
     * GuiBase のデフォルト実装は onMouseClicked が false を返すと
     * super.mouseClicked()（バニラ Screen）を呼ぶ。バニラ Screen は children を走査するが、
     * malilib は addDrawableChild を使わないため現状は二重にならない。
     * ただし将来の安全のため onMouseClicked を直接呼び出し、バニラ側を呼ばない。
     *
     * ScoreboardTab のスクロールバー（malilib 管理外）は先に処理する。
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == ConfigGuiTab.SCOREBOARD && scoreboardTab != null) {
            if (scoreboardTab.mouseClicked(mouseX, mouseY, button)) return true;
        }
        // malilib のボタン／ウィジェットループのみ実行（バニラ Screen は呼ばない）
        this.onMouseClicked((int) mouseX, (int) mouseY, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (tab == ConfigGuiTab.SCOREBOARD && scoreboardTab != null) {
            if (scoreboardTab.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (tab == ConfigGuiTab.SCOREBOARD && scoreboardTab != null) {
            if (scoreboardTab.mouseDragged(mouseX, mouseY, button, dx, dy)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (tab == ConfigGuiTab.SCOREBOARD && scoreboardTab != null) {
            if (scoreboardTab.mouseScrolled(mouseX, mouseY, amount)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    // ── ライフサイクル ──────────────────────────────────────────────────────────

    @Override
    public void removed() {
        if (scoreboardTab != null) {
            scoreboardTab.onClose();
            scoreboardTab = null;
        }
        super.removed();
        TweaksOptions.writeToConfig(ClientConfigManager.config);
        ClientConfigManager.save();
    }

    @Override
    protected void onSettingsChanged() {
        super.onSettingsChanged();
        TweaksOptions.writeToConfig(ClientConfigManager.config);
        ClientConfigManager.save();
    }

    private BooleanHotkeyGuiWrapper wrapConfig(fi.dy.masa.malilib.config.IHotkeyTogglable config) {
        return new BooleanHotkeyGuiWrapper(config.getPrettyName(), config, config.getKeybind());
    }

    // ── 内部クラス ───────────────────────────────────────────────────────────────

    private static class TabButtonListener implements IButtonActionListener {
        private final ConfigGuiTab targetTab;
        private final HikariTweaksConfigScreen gui;

        TabButtonListener(ConfigGuiTab targetTab, HikariTweaksConfigScreen gui) {
            this.targetTab = targetTab;
            this.gui       = gui;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            tab = this.targetTab;
            this.gui.reCreateListWidget();
            if (this.gui.getListWidget() != null) {
                this.gui.getListWidget().resetScrollbarPosition();
            }
            this.gui.initGui();
        }
    }

    private enum ConfigGuiTab {
        TWEAKS("補助機能"), LISTS("リスト"), HOTKEYS("ホットキー"), SCOREBOARD("スコアボード");
        private final String displayName;
        ConfigGuiTab(String d) { this.displayName = d; }
        public String getDisplayName() { return displayName; }
    }
}
