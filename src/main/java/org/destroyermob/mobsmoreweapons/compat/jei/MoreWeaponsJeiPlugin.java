package org.destroyermob.mobsmoreweapons.compat.jei;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.destroyermob.mobsmoreweapons.MoreWeapons;

@JeiPlugin
public final class MoreWeaponsJeiPlugin implements IModPlugin {
    private static final String MTF = "mobstoolforging";
    private static final List<ResourceLocation> TEMPLATES = List.of(
            modLoc("great_sword_blade"),
            modLoc("katana_blade"),
            modLoc("battle_axe_head"),
            modLoc("knife_blade"),
            modLoc("machete_blade"),
            modLoc("wide_guard")
    );

    @Override
    public ResourceLocation getPluginUid() {
        return modLoc("jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        if (!ModList.get().isLoaded(MTF)) {
            return;
        }
        try {
            Item templatePattern = mtfItem("TEMPLATE_PATTERN");
            DataComponentType<ResourceLocation> component = forgeTemplateComponent();
            registration.registerSubtypeInterpreter(templatePattern, new ISubtypeInterpreter<>() {
                @Override
                public Object getSubtypeData(ItemStack stack, UidContext context) {
                    return stack.get(component);
                }

                @Override
                @Deprecated
                public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                    ResourceLocation template = stack.get(component);
                    return template == null ? "" : template.toString();
                }
            });
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // MTF/JEI versions without the generic pattern item still work without this subtype hint.
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void registerRecipes(IRecipeRegistration registration) {
        if (!ModList.get().isLoaded(MTF)) {
            return;
        }
        try {
            Reflection refs = new Reflection();
            List recipes = new ArrayList<>();
            for (ResourceLocation templateId : TEMPLATES) {
                Object template = refs.template(templateId).orElse(null);
                if (template == null) {
                    continue;
                }
                for (ResourceLocation materialId : refs.starterMaterialIds()) {
                    refs.recipe(templateId, template, materialId).ifPresent(recipes::add);
                }
            }
            if (!recipes.isEmpty()) {
                RecipeType recipeType = RecipeType.create(MTF, "forge_shaping", refs.forgeShapingRecipeType);
                registration.addRecipes(recipeType, recipes);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // If MTF changes this JEI record again, the in-game recipes still work; only the extra JEI view drops out.
        }
    }

    @SuppressWarnings("unchecked")
    private static DataComponentType<ResourceLocation> forgeTemplateComponent() throws ReflectiveOperationException {
        Object holder = Class.forName("org.destroyermob.mobstoolforging.registry.ModDataComponents")
                .getField("FORGE_TEMPLATE")
                .get(null);
        return (DataComponentType<ResourceLocation>) holder.getClass().getMethod("get").invoke(holder);
    }

    private static Item mtfItem(String fieldName) throws ReflectiveOperationException {
        Object holder = Class.forName("org.destroyermob.mobstoolforging.registry.ModItems")
                .getField(fieldName)
                .get(null);
        return (Item) holder.getClass().getMethod("get").invoke(holder);
    }

    private static ItemStack pattern(ResourceLocation templateId) throws ReflectiveOperationException {
        ItemStack stack = new ItemStack(mtfItem("TEMPLATE_PATTERN"));
        stack.set(forgeTemplateComponent(), templateId);
        return stack;
    }

    private static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, path);
    }

    private static final class Reflection {
        private final Class<?> forgeShapingRecipeType = Class.forName("org.destroyermob.mobstoolforging.integration.jei.ForgeShapingJeiRecipe");
        private final Class<?> workstationKindType = Class.forName("org.destroyermob.mobstoolforging.world.WorkstationKind");
        private final Class<?> forgeTemplateDefinitionType = Class.forName("org.destroyermob.mobstoolforging.world.ForgeTemplateDefinition");
        private final Constructor<?> forgeShapingRecipe = forgeShapingRecipeType.getConstructor(
                ResourceLocation.class,
                workstationKindType,
                forgeTemplateDefinitionType,
                ResourceLocation.class,
                ItemStack.class,
                ItemStack.class,
                ItemStack.class,
                ItemStack.class,
                ItemStack.class,
                ItemStack.class,
                int.class,
                int.class
        );
        private final Method registryTemplate = Class.forName("org.destroyermob.mobstoolforging.world.ToolTypeRegistry")
                .getMethod("template", ResourceLocation.class);
        private final Method materialDefinition = Class.forName("org.destroyermob.mobstoolforging.world.MaterialCatalog")
                .getMethod("definition", ResourceLocation.class);
        private final Method starterMaterialIds = Class.forName("org.destroyermob.mobstoolforging.world.MaterialCatalog")
                .getMethod("starterMaterialIds");
        private final Method allowsMaterial = forgeTemplateDefinitionType.getMethod("allowsMaterial", ResourceLocation.class);
        private final Method outputStack = forgeTemplateDefinitionType.getMethod("outputStack", ResourceLocation.class);
        private final Method requiredMaterials = forgeTemplateDefinitionType.getMethod("requiredMaterials");
        private final Method requiredHits = forgeTemplateDefinitionType.getMethod("requiredHits");
        private final Method minimumHammerLevel = forgeTemplateDefinitionType.getMethod("minimumHammerLevel", ResourceLocation.class);
        private final Method materialCategory;
        private final Method materialDisplayItem;
        private final Object toolForgeKind = enumConstant(workstationKindType, "TOOL_FORGE");
        private final Object lapidaryKind = enumConstant(workstationKindType, "LAPIDARY_TABLE");
        private final int ironHammerLevel = ironHammerLevel();

        private Reflection() throws ReflectiveOperationException {
            Class<?> materialDefinitionType = Class.forName("org.destroyermob.mobstoolforging.world.ToolMaterialDefinition");
            materialCategory = materialDefinitionType.getMethod("category");
            materialDisplayItem = materialDefinitionType.getMethod("displayItem");
        }

        @SuppressWarnings("unchecked")
        private Optional<Object> template(ResourceLocation templateId) throws ReflectiveOperationException {
            return (Optional<Object>) registryTemplate.invoke(null, templateId);
        }

        @SuppressWarnings("unchecked")
        private List<ResourceLocation> starterMaterialIds() throws ReflectiveOperationException {
            return (List<ResourceLocation>) starterMaterialIds.invoke(null);
        }

        private Optional<Object> recipe(ResourceLocation templateId, Object template, ResourceLocation materialId) {
            try {
                if (!(Boolean) allowsMaterial.invoke(template, materialId)) {
                    return Optional.empty();
                }
                Object material = ((Optional<?>) materialDefinition.invoke(null, materialId)).orElse(null);
                if (material == null) {
                    return Optional.empty();
                }
                ItemStack output = (ItemStack) outputStack.invoke(template, materialId);
                if (output.isEmpty()) {
                    return Optional.empty();
                }

                boolean gem = materialCategory.invoke(material).toString().equals("GEM");
                int minimumLevel = (Integer) minimumHammerLevel.invoke(template, materialId);
                Object workstation = gem ? lapidaryKind : toolForgeKind;
                ItemStack station = new ItemStack(mtfItem(gem ? "LAPIDARY_TABLE" : "TOOL_FORGE"));
                ItemStack materialStack = new ItemStack((Item) materialDisplayItem.invoke(material), (Integer) requiredMaterials.invoke(template));
                ItemStack catalyst = gem ? new ItemStack(mtfItem("DIAMOND_POWDER")) : ItemStack.EMPTY;
                ItemStack hammer = new ItemStack(mtfItem(minimumLevel >= ironHammerLevel ? "IRON_SMITHING_HAMMER" : "SMITHING_HAMMER"));

                return Optional.of(forgeShapingRecipe.newInstance(
                        recipeId(templateId, materialId),
                        workstation,
                        template,
                        materialId,
                        station,
                        pattern(templateId),
                        materialStack,
                        catalyst,
                        hammer,
                        output,
                        (Integer) requiredHits.invoke(template),
                        minimumLevel
                ));
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return Optional.empty();
            }
        }

        private static Object enumConstant(Class<?> enumType, String name) {
            for (Object constant : enumType.getEnumConstants()) {
                if (((Enum<?>) constant).name().equals(name)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException("Missing enum constant " + enumType.getName() + "." + name);
        }

        private static int ironHammerLevel() throws ReflectiveOperationException {
            Class<?> hammerLevelType = Class.forName("org.destroyermob.mobstoolforging.world.SmithingHammerLevel");
            Object iron = enumConstant(hammerLevelType, "IRON");
            return (Integer) hammerLevelType.getMethod("level").invoke(iron);
        }

        private static ResourceLocation recipeId(ResourceLocation templateId, ResourceLocation materialId) {
            String materialPath = materialId.getNamespace().equals(MTF)
                    ? materialId.getPath()
                    : materialId.getNamespace() + "/" + materialId.getPath();
            return ResourceLocation.fromNamespaceAndPath(MoreWeapons.MOD_ID, "forge_shaping/" + templateId.getPath() + "/" + materialPath);
        }
    }
}
