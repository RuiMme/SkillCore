package com.rui.skillcore.libs.util;

import com.mojang.blaze3d.vertex.IVertexBuilder;

// ==========================================
// 底层黑魔法：用于强行修改物品透明度的顶点拦截器
// ==========================================
public class AlphaVertexBuilder implements IVertexBuilder {
    private final IVertexBuilder delegate;
    private final int alpha;

    public AlphaVertexBuilder(IVertexBuilder delegate, float alpha) {
        this.delegate = delegate;
        // 将 0.0~1.0 的浮点透明度转换为 0~255 的整数
        this.alpha = (int) Math.max(0, Math.min(255, alpha * 255));
    }

    // 注意：由于不同的 Forge Mappings 名字可能略有不同（例如 pos 和 vertex，tex 和 uv）
    // 如果你的 IDE 在这里报错，请直接删掉这些方法，用 IDE 的快捷键 (Alt+Enter) 重新“实现接口方法”
    // 然后在 color 方法里加上 `(a * this.alpha) / 255` 即可。

    @Override
    public IVertexBuilder vertex(double x, double y, double z) {
        this.delegate.vertex(x, y, z); return this;
    }

    @Override
    public IVertexBuilder color(int r, int g, int b, int a) {
        // 【核心逻辑】：将原模型顶点自带的透明度，乘以我们传入的虚影透明度
        this.delegate.color(r, g, b, (a * this.alpha) / 255);
        return this;
    }

    @Override
    public IVertexBuilder uv(float u, float v) {
        this.delegate.uv(u, v); return this;
    }

    @Override
    public IVertexBuilder overlayCoords(int u, int v) {
        this.delegate.overlayCoords(u, v); return this;
    }

    @Override
    public IVertexBuilder uv2(int u, int v) {
        this.delegate.uv2(u, v); return this;
    }

    @Override
    public IVertexBuilder normal(float x, float y, float z) {
        this.delegate.normal(x, y, z); return this;
    }

    @Override
    public void endVertex() {
        this.delegate.endVertex();
    }
}
