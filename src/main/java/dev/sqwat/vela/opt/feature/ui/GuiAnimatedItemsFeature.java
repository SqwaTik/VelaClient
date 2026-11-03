package dev.sqwat.vela.opt.feature.ui;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "gui_animated_items",
	group = FeatureGroup.INTERFACE,
	mixins = {"hud.GuiRendererMixin", "hud.GuiItemAtlasMixin"}
)
public class GuiAnimatedItemsFeature extends Feature {
}
