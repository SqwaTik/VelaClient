package dev.sqwat.vela.compat.pipeline;

/** 1.21.5's depth test functions, as OpenGL comparison functions. */
public enum DepthTestFunction {
    NO_DEPTH_TEST(519), EQUAL_DEPTH_TEST(514), LEQUAL_DEPTH_TEST(515), LESS_DEPTH_TEST(513), GREATER_DEPTH_TEST(516);
    final int gl;
    DepthTestFunction(int gl){this.gl=gl;}
}
