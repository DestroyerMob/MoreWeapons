package org.destroyermob.mobsmoreweapons.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.destroyermob.mobsmoreweapons.MoreWeapons;
import org.destroyermob.mobsmoreweapons.item.ModItems;

public final class MobsToolForgingCompat {
    private static final String MTF = "mobstoolforging";
    private static final ResourceLocation IRON = mtfLoc("iron");
    private static final ResourceLocation GOLD = mtfLoc("gold");
    private static final ResourceLocation DIAMOND = mtfLoc("diamond");
    private static final int DEFAULT_QUALITY = 100;

    private static final Map<String, Map<ResourceLocation, Supplier<? extends Item>>> TOOLS = new LinkedHashMap<>();
    private static final Map<String, Map<ResourceLocation, Supplier<? extends Item>>> PARTS = new LinkedHashMap<>();

    static {
        registerToolItems("great_sword", ModItems.IRONGREATSWORD::get, ModItems.GOLDGREATSWORD::get, ModItems.DIAMONDGREATSWORD::get);
        registerToolItems("katana", ModItems.IRONKATANA::get, ModItems.GOLDKATANA::get, ModItems.DIAMONDKATANA::get);
        registerToolItems("battle_axe", ModItems.IRONBATTLEAXE::get, ModItems.GOLDBATTLEAXE::get, ModItems.DIAMONDBATTLEAXE::get);
        registerToolItems("knife", ModItems.IRONKNIFE::get, ModItems.GOLDENKNIFE::get, ModItems.DIAMONDKNIFE::get);
        registerToolItems("machete", ModItems.IRONMACHETE::get, ModItems.GOLDENMACHETE::get, ModItems.DIAMONDMACHETE::get);

        registerPartItems("great_sword_blade", ModItems.IRONGREATSWORDBLADE::get, ModItems.GOLDENGREATSWORDBLADE::get, ModItems.DIAMONDGREATSWORDBLADE::get);
        registerPartItems("katana_blade", ModItems.IRONKATANABLADE::get, ModItems.GOLDENKATANABLADE::get, ModItems.DIAMONDKATANABLADE::get);
        registerPartItems("battle_axe_head", ModItems.IRONBATTLEAXEHEAD::get, ModItems.GOLDENBATTLEAXEHEAD::get, ModItems.DIAMONDBATTLEAXEHEAD::get);
        registerPartItems("knife_blade", ModItems.IRONKNIFEBLADE::get, ModItems.GOLDENKNIFEBLADE::get, ModItems.DIAMONDKNIFEBLADE::get);
        registerPartItems("machete_blade", ModItems.IRONMACHETEBLADE::get, ModItems.GOLDENMACHETEBLADE::get, ModItems.DIAMONDMACHETEBLADE::get);
        registerPartItems("wide_guard", ModItems.IRONWIDEGUARD::get, ModItems.GOLDENWIDEGUARD::get, ModItems.DIAMONDWIDEGUARD::get);
    }

    private MobsToolForgingCompat() {
    }

    public static void register() {
        try {
            Reflection refs = new Reflection();
            registerToolType(refs, "great_sword", "great_sword_blade", 12.0F, -3.5F, true);
            registerToolType(refs, "katana", "katana_blade", 2.0F, -2.1F, true);
            registerToolType(refs, "battle_axe", "battle_axe_head", 7.0F, -3.0F, false);
            registerToolType(refs, "knife", "knife_blade", 1.0F, -2.0F, false);
            registerToolType(refs, "machete", "machete_blade", 5.0F, -2.7F, false);
            registerBattleAxeDiamondStatFix(refs);
        } catch (ReflectiveOperationException | LinkageError exception) {
            MoreWeapons.LOGGER.warn("Mobs Tool Forging compatibility could not be registered.", exception);
        }
    }

    private static void registerToolType(Reflection refs, String toolName, String primaryPartType, float attackDamage, float attackSpeed, boolean needsWideGuard)
            throws ReflectiveOperationException {
        ResourceLocation toolId = modLoc(toolName);
        Object builder = refs.toolTypeBuilder.invoke(null, toolId, primaryPartType);
        invoke(builder, "visual", new Class<?>[]{ResourceLocation.class}, toolId);
        invoke(builder, "baseStats", new Class<?>[]{float.class, float.class}, attackDamage, attackSpeed);
        invoke(builder, "swordLike", new Class<?>[]{boolean.class}, true);
        invoke(builder, "toolFactory", new Class<?>[]{BiFunction.class}, toolFactory(refs, toolName));
        invoke(builder, "partFactory", new Class<?>[]{refs.partFactoryType}, partFactory(refs));
        if (needsWideGuard) {
            addRequiredPart(builder, "wide_guard");
        }

        Object definition = invoke(builder, "build", new Class<?>[0]);
        refs.registerToolType.invoke(null, definition);
    }

    private static BiFunction<Object, Object, ItemStack> toolFactory(Reflection refs, String toolName) {
        return (definition, construction) -> {
            try {
                ResourceLocation material = refs.headMaterial(construction);
                Supplier<? extends Item> item = TOOLS.getOrDefault(toolName, Map.of()).get(material);
                if (item == null) {
                    return ItemStack.EMPTY;
                }
                ItemStack stack = new ItemStack(item.get());
                stack.set(refs.toolConstructionComponent(), construction);
                refs.applyStats.invoke(null, stack, definition, construction);
                return stack;
            } catch (ReflectiveOperationException exception) {
                MoreWeapons.LOGGER.warn("Failed to create Mobs Tool Forging {} stack.", toolName, exception);
                return ItemStack.EMPTY;
            }
        };
    }

    private static Object partFactory(Reflection refs) {
        return Proxy.newProxyInstance(
                refs.partFactoryType.getClassLoader(),
                new Class<?>[]{refs.partFactoryType},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(proxy, method, args);
                    }
                    String partType = (String) args[1];
                    ResourceLocation material = (ResourceLocation) args[2];
                    int quality = (Integer) args[3];
                    Supplier<? extends Item> item = PARTS.getOrDefault(partType, Map.of()).get(material);
                    if (item == null) {
                        return ItemStack.EMPTY;
                    }
                    ItemStack stack = new ItemStack(item.get());
                    stack.set(refs.toolPartComponent(), refs.newToolPart(partType, material, quality));
                    return stack;
                }
        );
    }

    @SuppressWarnings("unchecked")
    private static void addRequiredPart(Object builder, String partType) throws ReflectiveOperationException {
        Field field = builder.getClass().getDeclaredField("requiredAssemblyParts");
        field.setAccessible(true);
        ((List<String>) field.get(builder)).add(partType);
    }

    private static void registerBattleAxeDiamondStatFix(Reflection refs) throws ReflectiveOperationException {
        Object modifier = Proxy.newProxyInstance(
                refs.statModifierType.getClassLoader(),
                new Class<?>[]{refs.statModifierType},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return objectMethod(proxy, method, args);
                    }
                    ResourceLocation toolId = refs.definitionId(args[0]);
                    ResourceLocation material = refs.headMaterial(args[1]);
                    if (modLoc("battle_axe").equals(toolId) && DIAMOND.equals(material)) {
                        refs.addAttackDamage.invoke(args[2], -1.0F);
                    }
                    return null;
                }
        );
        refs.registerStatModifier.invoke(null, modifier);
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "MobsMoreWeapons MobsToolForging bridge proxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> args != null && args.length == 1 && proxy == args[0];
            default -> null;
        };
    }

    private static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... args) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(name, parameterTypes);
        return method.invoke(target, args);
    }

    private static void registerToolItems(String toolName, Supplier<? extends Item> iron, Supplier<? extends Item> gold, Supplier<? extends Item> diamond) {
        TOOLS.put(toolName, materialMap(iron, gold, diamond));
    }

    private static void registerPartItems(String partName, Supplier<? extends Item> iron, Supplier<? extends Item> gold, Supplier<? extends Item> diamond) {
        PARTS.put(partName, materialMap(iron, gold, diamond));
    }

    private static Map<ResourceLocation, Supplier<? extends Item>> materialMap(Supplier<? extends Item> iron, Supplier<? extends Item> gold, Supplier<? extends Item> diamond) {
        Map<ResourceLocation, Supplier<? extends Item>> values = new LinkedHashMap<>();
        values.put(IRON, iron);
        values.put(GOLD, gold);
        values.put(DIAMOND, diamond);
        return Map.copyOf(values);
    }

    private static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, path);
    }

    private static ResourceLocation mtfLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MTF, path);
    }

    private static final class Reflection {
        private final Class<?> partFactoryType;
        private final Class<?> statModifierType;
        private final Method toolTypeBuilder;
        private final Method registerToolType;
        private final Method registerStatModifier;
        private final Method headMaterial;
        private final Method definitionId;
        private final Method applyStats;
        private final Method addAttackDamage;
        private final Constructor<?> toolPartDataConstructor;
        private final DataComponentType<Object> toolConstructionComponent;
        private final DataComponentType<Object> toolPartComponent;

        @SuppressWarnings("unchecked")
        private Reflection() throws ReflectiveOperationException {
            Class<?> registryType = Class.forName("org.destroyermob.mobstoolforging.world.ToolTypeRegistry");
            Class<?> toolTypeDefinitionType = Class.forName("org.destroyermob.mobstoolforging.world.ToolTypeDefinition");
            Class<?> toolConstructionDataType = Class.forName("org.destroyermob.mobstoolforging.world.ToolConstructionData");
            Class<?> toolPartDataType = Class.forName("org.destroyermob.mobstoolforging.world.ToolPartData");
            partFactoryType = Class.forName("org.destroyermob.mobstoolforging.world.ToolTypeDefinition$PartFactory");
            statModifierType = Class.forName("org.destroyermob.mobstoolforging.world.ToolTypeRegistry$ToolStatModifier");
            Class<?> statBuilderType = Class.forName("org.destroyermob.mobstoolforging.world.ToolStatBuilder");
            Class<?> mutableStatsType = Class.forName("org.destroyermob.mobstoolforging.world.ToolStatBuilder$MutableStats");
            Class<?> dataComponentsType = Class.forName("org.destroyermob.mobstoolforging.registry.ModDataComponents");

            toolTypeBuilder = toolTypeDefinitionType.getMethod("builder", ResourceLocation.class, String.class);
            registerToolType = registryType.getMethod("registerToolType", toolTypeDefinitionType);
            registerStatModifier = registryType.getMethod("registerStatModifier", statModifierType);
            headMaterial = toolConstructionDataType.getMethod("headMaterial");
            definitionId = toolTypeDefinitionType.getMethod("id");
            applyStats = statBuilderType.getMethod("apply", ItemStack.class, toolTypeDefinitionType, toolConstructionDataType);
            addAttackDamage = mutableStatsType.getMethod("addAttackDamage", float.class);
            toolPartDataConstructor = toolPartDataType.getConstructor(String.class, ResourceLocation.class, int.class);

            toolConstructionComponent = (DataComponentType<Object>) component(dataComponentsType, "TOOL_CONSTRUCTION");
            toolPartComponent = (DataComponentType<Object>) component(dataComponentsType, "TOOL_PART");
        }

        private ResourceLocation headMaterial(Object construction) throws ReflectiveOperationException {
            return (ResourceLocation) headMaterial.invoke(construction);
        }

        private ResourceLocation definitionId(Object definition) throws ReflectiveOperationException {
            return (ResourceLocation) definitionId.invoke(definition);
        }

        private Object newToolPart(String partType, ResourceLocation material, int quality) throws ReflectiveOperationException {
            return toolPartDataConstructor.newInstance(partType, material, quality <= 0 ? DEFAULT_QUALITY : quality);
        }

        private DataComponentType<Object> toolConstructionComponent() {
            return toolConstructionComponent;
        }

        private DataComponentType<Object> toolPartComponent() {
            return toolPartComponent;
        }

        private static Object component(Class<?> dataComponentsType, String fieldName) throws ReflectiveOperationException {
            Object holder = dataComponentsType.getField(fieldName).get(null);
            return ((Supplier<?>) holder).get();
        }
    }
}
