package dev.sqwat.vela.opt.entity;

import lombok.experimental.UtilityClass;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;
import dev.sqwat.vela.opt.gl.RawBuffer;
import dev.sqwat.vela.opt.mixin.entity.ModelFeatureRendererInvoker;
import dev.sqwat.vela.opt.mixin.entity.RenderTypeFeatureRendererInvoker;

// 26.2 hands models over in groups that are already ordered and batched; a group is one parallel batch
@UtilityClass
public class ParallelModelHook {
	private final List<ParallelModels.Job> JOBS = new ArrayList<>();

	// false: too few models in this group, vanilla builds it
	public boolean buildGroup(ModelFeatureRenderer renderer, List<ModelFeatureRenderer.Submit<?>> submits) {
		if (!ParallelModels.enabled() || submits.size() < ParallelModels.MIN_SUBMITS) {
			return false;
		}
		JOBS.clear();
		RenderTypeFeatureRendererInvoker builders = (RenderTypeFeatureRendererInvoker) renderer;
		ModelFeatureRendererInvoker vanilla = (ModelFeatureRendererInvoker) renderer;
		if (direct(submits, builders, vanilla)) {
			return true;
		}
		ParallelModels.begin();
		try {
			for (ModelFeatureRenderer.Submit<?> s : submits) {
				// block cracks go through a decal generator: those few go the vanilla way
				JOBS.add(s.sheetedDecalPose() != null || s.renderType().format() != ParallelModels.FORMAT || AnimFreeze.freezes(s.state(), s.pose().pose()) ? null
						: ParallelModels.add(s.model(), s.state(), s.pose(), s.lightCoords(), s.overlayCoords(), s.tintedColor(), s.sprite()));
			}
		} finally {
			ParallelModels.end();
		}
		try (MemoryStack stack = MemoryStack.stackPush()) {
			for (int k = 0; k < submits.size(); k++) {
				ModelFeatureRenderer.Submit<?> s = submits.get(k);
				ParallelModels.Job job = JOBS.get(k);
				// one builder call per model, as vanilla does: it decides where a new draw starts
				if (job == null || !ParallelModels.emit(job, builders.velaopt$vertexBuilder(s.renderType()), stack)) {
					vanilla.velaopt$prepareModel(s);
				}
			}
		}
		return true;
	}

	private boolean eligible(ModelFeatureRenderer.Submit<?> s) {
		return s.sheetedDecalPose() == null && s.renderType().format() == ParallelModels.FORMAT && !s.renderType().sortOnUpload()
				&& !AnimFreeze.freezes(s.state(), s.pose().pose());
	}

	// The common case (a herd of plain models in vanilla's buffers): every model gets its room in the frame's vertex
	// buffer right away, in vanilla's order, and the workers fill it in place. All models share the group's one staging
	// buffer, so it is grown once up front for the most they can take: it never moves while a worker writes into it.
	// false: something here needs the copying path (3D skin layers, block cracks, a sorted or foreign buffer), nothing was done
	private boolean direct(List<ModelFeatureRenderer.Submit<?>> submits, RenderTypeFeatureRendererInvoker builders, ModelFeatureRendererInvoker vanilla) {
		if (SkinLayers.ACTIVE) {
			return false;
		}
		long bytes = 0;
		for (ModelFeatureRenderer.Submit<?> s : submits) {
			if (!eligible(s)) {
				return false;
			}
			bytes += ParallelModels.maxBytes(s.model());
		}
		if (!(builders.velaopt$vertexBuilder(submits.get(0).renderType()) instanceof RawBuffer first) || !first.velaopt$accepts(ParallelModels.FORMAT)) {
			return false;
		}
		first.velaopt$ensureFree(bytes);
		int k = 0;
		ParallelModels.begin();
		try {
			for (; k < submits.size(); k++) {
				ModelFeatureRenderer.Submit<?> s = submits.get(k);
				// one builder call per model, as vanilla does: it decides where a new draw starts (the first one is above)
				VertexConsumer consumer = k == 0 ? (VertexConsumer) first : builders.velaopt$vertexBuilder(s.renderType());
				if (!(consumer instanceof RawBuffer raw) || !raw.velaopt$accepts(ParallelModels.FORMAT)) {
					break;
				}
				ParallelModels.add(s.model(), s.state(), s.pose(), s.lightCoords(), s.overlayCoords(), s.tintedColor(), s.sprite(),
						ParallelModels.FORMAT, raw);
			}
		} finally {
			ParallelModels.end();
		}
		// a buffer that cannot take raw vertices after all: the workers are done, the rest goes the vanilla way, still in order
		for (; k < submits.size(); k++) {
			vanilla.velaopt$prepareModel(submits.get(k));
		}
		return true;
	}
}
