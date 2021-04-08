package tallestegg.bigbrain;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import tallestegg.bigbrain.entity.IBucklerUser;
import tallestegg.bigbrain.items.BucklerItem;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = BigBrain.MODID)
public class BigBrainClientEvents {
    public static final Method preRenderCallback = ObfuscationReflectionHelper.findMethod(LivingRenderer.class, "func_225620_a_", LivingEntity.class, MatrixStack.class, float.class);

    @SubscribeEvent
    public static void onMovementKeyPressed(InputUpdateEvent event) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (((IBucklerUser) player).isBucklerDashing()) {
            event.getMovementInput().jump = false;
            event.getMovementInput().moveStrafe = 0;
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        MatrixStack mStack = event.getMatrixStack();
        ItemStack stack = event.getItemStack();
        ClientPlayerEntity player = Minecraft.getInstance().player;
        float partialTicks = event.getPartialTicks();
        if (stack.getItem() instanceof BucklerItem && (player.isHandActive() && player.getActiveItemStack() == stack || ((IBucklerUser) player).isBucklerDashing())) {
            boolean mainHand = event.getHand() == Hand.MAIN_HAND;
            HandSide handside = mainHand ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
            boolean rightHand = handside == HandSide.RIGHT;
            float f7 = (float) stack.getUseDuration() - ((float) player.getItemInUseCount() - partialTicks + 1.0F);
            float f11 = f7 / 10.0F;
            if (f11 > 1.0F) {
                f11 = 1.0F;
            }
            mStack.translate(f11 * 0.2D, 0.0D, f11 * 0.2D);
            mStack.rotate(Vector3f.YP.rotationDegrees((boolean) rightHand ? f11 : -f11 * 0.2F));
        }
    }

    @SubscribeEvent
    public static void onEntityRenderPre(RenderLivingEvent.Post<LivingEntity, EntityModel<LivingEntity>> event) {
        LivingEntity entityIn = (LivingEntity) event.getEntity();
        LivingRenderer<LivingEntity, EntityModel<LivingEntity>> renderer = event.getRenderer();
        EntityModel<LivingEntity> model = renderer.getEntityModel();
        MatrixStack stack = event.getMatrixStack();
        if (!BigBrainConfig.RenderAfterImage)
            return;
        if (((IBucklerUser) entityIn).isBucklerDashing()) {
            for (int i = 0; i < 5; i++) {
                if (i != 0) {
                    stack.push();
                    model.swingProgress = entityIn.getSwingProgress(event.getPartialRenderTick());
                    boolean shouldSit = entityIn.isPassenger() && (entityIn.getRidingEntity() != null && entityIn.getRidingEntity().shouldRiderSit());
                    model.isSitting = shouldSit;
                    model.isChild = entityIn.isChild();
                    float f = MathHelper.interpolateAngle(event.getPartialRenderTick(), entityIn.prevRenderYawOffset, entityIn.renderYawOffset);
                    float f1 = MathHelper.interpolateAngle(event.getPartialRenderTick(), entityIn.prevRotationYawHead, entityIn.rotationYawHead);
                    float f2 = f1 - f;
                    if (shouldSit && entityIn.getRidingEntity() instanceof LivingEntity) {
                        LivingEntity livingentity = (LivingEntity) entityIn.getRidingEntity();
                        f = MathHelper.interpolateAngle(event.getPartialRenderTick(), livingentity.prevRenderYawOffset, livingentity.renderYawOffset);
                        f2 = f1 - f;
                        float f3 = MathHelper.wrapDegrees(f2);
                        if (f3 < -85.0F) {
                            f3 = -85.0F;
                        }

                        if (f3 >= 85.0F) {
                            f3 = 85.0F;
                        }

                        f = f1 - f3;
                        if (f3 * f3 > 2500.0F) {
                            f += f3 * 0.2F;
                        }

                        f2 = f1 - f;
                    }

                    float f6 = MathHelper.lerp(event.getPartialRenderTick(), entityIn.prevRotationPitch, entityIn.rotationPitch);
                    if (entityIn.getPose() == Pose.SLEEPING) {
                        Direction direction = entityIn.getBedDirection();
                        if (direction != null) {
                            float f4 = entityIn.getEyeHeight(Pose.STANDING) - 0.1F;
                            stack.translate((double) ((float) (-direction.getXOffset()) * f4), 0.0D, (double) ((float) (-direction.getZOffset()) * f4));
                        }
                    }
                    float f7 = (float) entityIn.ticksExisted + event.getPartialRenderTick();
                    stack.rotate(Vector3f.YP.rotationDegrees(180.0F - f));
                    try {
                        preRenderCallback.invoke(renderer, entityIn, stack, event.getPartialRenderTick());
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        new RuntimeException("Big Brain has failed to invoke preRenderCallback via reflection.");
                    }
                    stack.scale(-1.0F, -1.0F, 1.0F);
                    double motionZ = Math.abs(entityIn.getMotion().getZ());
                    stack.translate(0.0D, (double) -1.501F, i * motionZ * 4 / ((IBucklerUser) entityIn).getBucklerUseTimer());
                    float f8 = 0.0F;
                    float f5 = 0.0F;
                    if (!shouldSit && entityIn.isAlive()) {
                        f8 = MathHelper.lerp(event.getPartialRenderTick(), entityIn.prevLimbSwingAmount, entityIn.limbSwingAmount);
                        f5 = entityIn.limbSwing - entityIn.limbSwingAmount * (1.0F - event.getPartialRenderTick());
                        if (entityIn.isChild()) {
                            f5 *= 3.0F;
                        }

                        if (f8 > 1.0F) {
                            f8 = 1.0F;
                        }
                    }

                    model.setLivingAnimations(entityIn, f5, f8, event.getPartialRenderTick());
                    model.setRotationAngles(entityIn, f5, f8, f7, f2, f6);
                    Minecraft minecraft = Minecraft.getInstance();
                    boolean flag = !entityIn.isInvisible();
                    boolean flag1 = !flag && !entityIn.isInvisibleToPlayer(minecraft.player);
                    boolean flag2 = minecraft.isEntityGlowing(entityIn);
                    RenderType rendertype = BigBrainClientEvents.getRenderType(entityIn, renderer, model, flag, flag1, flag2);
                    if (rendertype != null) {
                        IVertexBuilder ivertexbuilder = event.getBuffers().getBuffer(rendertype);
                        int overlay = LivingRenderer.getPackedOverlay(entityIn, 0.0F);
                        model.render(stack, ivertexbuilder, event.getLight(), overlay, 1.0F, 1.0F, 1.0F, 0.3F / i + 1.0F);
                    }
                    if (!entityIn.isSpectator()) {
                        if (BigBrainConfig.RenderEntityLayersDuringAfterImage) {
                            for (LayerRenderer<LivingEntity, EntityModel<LivingEntity>> layerrenderer : renderer.layerRenderers) {
                                layerrenderer.render(stack, event.getBuffers(), event.getLight(), entityIn, f5, f8, event.getPartialRenderTick(), f7, f2, f6);
                            }
                        }
                    }
                    stack.pop();
                }
            }
        }
    }

    public static RenderType getRenderType(LivingEntity p_230496_1_, LivingRenderer<LivingEntity, ?> renderer, EntityModel<?> model, boolean p_230496_2_, boolean p_230496_3_, boolean p_230496_4_) {
        ResourceLocation resourcelocation = renderer.getEntityTexture(p_230496_1_);
        if (p_230496_3_) {
            return RenderType.getItemEntityTranslucentCull(resourcelocation);
        } else if (p_230496_2_) {
            return RenderType.getEntityTranslucent(resourcelocation);
        } else {
            return p_230496_4_ ? RenderType.getOutline(resourcelocation) : null;
        }
    }
}
