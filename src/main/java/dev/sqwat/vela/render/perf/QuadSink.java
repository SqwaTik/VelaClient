package dev.sqwat.vela.render.perf;

/** A BufferBuilder that can hand out room for whole vertices at once (implemented by BufferBuilderQuadMixin). */
public interface QuadSink {
    /** True while it builds the vanilla entity format (not lines, which repeat vertices), the layout {@link FastCubes} writes. */
    boolean vela$entityQuads();

    /** True while it builds quads of the text format (position, colour, texture, light: 28 bytes), the layout of {@link FastCubes#textQuad}. */
    boolean vela$textQuads();

    /** Room for {@code count} complete vertices, already counted; the caller fills every byte but the normal's padding. */
    long vela$reserveVertices(int count);
}
