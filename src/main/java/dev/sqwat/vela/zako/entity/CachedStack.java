// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.entity;

import lombok.experimental.UtilityClass;
import org.lwjgl.system.MemoryStack;

// MemoryStack.stackPush() without the ThreadLocal lookup for the thread that called last (the render thread)
@UtilityClass
public class CachedStack {
	private record ThreadStack(Thread thread, MemoryStack stack) {
	}

	// one record so another thread never sees a thread/stack pair half-updated
	private ThreadStack cached = new ThreadStack(null, null);

	public MemoryStack push() {
		ThreadStack current = cached;
		Thread thread = Thread.currentThread();
		if (current.thread != thread) {
			current = new ThreadStack(thread, MemoryStack.stackGet());
			cached = current;
		}
		return current.stack.push();
	}
}
