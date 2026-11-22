package dev.sqwat.vela.opt.entity;

import dev.sqwat.vela.opt.feature.Features;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

// 26.2 sorts the quads of every translucent draw by distance on upload, every frame. Player skins (all on the skin
// atlas) and dropped items are mostly opaque: skipping that sort only reorders the odd see-through pixel.
@UtilityClass
public class UploadSort {
	private RenderType skins;

	public boolean sorted(RenderType type) {
		if (!type.sortOnUpload()) {
			return false;
		}
		if (Features.NO_ENTITY_SORT.on && type == skins()) {
			return false;
		}
		return !(Features.NO_ITEM_SORT.on && (type == Sheets.translucentItemSheet() || type == Sheets.translucentBlockItemSheet()));
	}

	// render types are memoized: the atlas one is the same instance every frame
	private RenderType skins() {
		RenderType s = skins;
		if (s == null) {
			s = skins = RenderTypes.entityTranslucent(SkinAtlas.ID);
		}
		return s;
	}
}
