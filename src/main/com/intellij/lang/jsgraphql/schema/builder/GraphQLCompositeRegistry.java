package com.intellij.lang.jsgraphql.schema.builder;

import com.intellij.lang.jsgraphql.schema.GraphQLSchemaUtil;
import com.intellij.lang.jsgraphql.types.GraphQLException;
import com.intellij.lang.jsgraphql.types.language.*;
import com.intellij.lang.jsgraphql.types.schema.idl.TypeDefinitionRegistry;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GraphQLCompositeRegistry {
    private static final Logger LOG = Logger.getInstance(GraphQLCompositeRegistry.class);

    private final Map<String, GraphQLCompositeDefinition<?>> myNamedCompositeDefinitions = new HashMap<>();
    private final GraphQLSchemaTypeCompositeDefinition mySchemaCompositeDefinition = new GraphQLSchemaTypeCompositeDefinition();

    public void merge(@NotNull TypeDefinitionRegistry source) throws GraphQLException {
        if (source.schemaDefinition().isPresent()) {
            addTypeDefinition(source.schemaDefinition().get());
        }

        source.types().values().forEach(this::addTypeDefinition);
        source.getDirectiveDefinitions().values().forEach(this::addTypeDefinition);
        source.scalars().values().forEach(this::addTypeDefinition);

        source.getSchemaExtensionDefinitions().forEach(this::addExtensionDefinition);
        source.objectTypeExtensions().forEach((key, value) -> value.forEach(this::addExtensionDefinition));
        source.interfaceTypeExtensions().forEach((key, value) -> value.forEach(this::addExtensionDefinition));
        source.unionTypeExtensions().forEach((key, value) -> value.forEach(this::addExtensionDefinition));
        source.enumTypeExtensions().forEach((key, value) -> value.forEach(this::addExtensionDefinition));
        source.scalarTypeExtensions().forEach((key, value) -> value.forEach(this::addExtensionDefinition));
        source.inputObjectTypeExtensions().forEach((key, value) -> value.forEach(this::addExtensionDefinition));
    }

    @NotNull
    private static GraphQLCompositeDefinition<?> createCompositeDefinition(@NotNull SDLDefinition<?> definition) {
        if (definition instanceof InputObjectTypeDefinition) {
            return new GraphQLInputObjectTypeCompositeDefinition();
        } else if (definition instanceof ObjectTypeDefinition) {
            return new GraphQLObjectTypeCompositeDefinition();
        } else if (definition instanceof InterfaceTypeDefinition) {
            return new GraphQLInterfaceTypeCompositeDefinition();
        } else if (definition instanceof UnionTypeDefinition) {
            return new GraphQLUnionTypeCompositeDefinition();
        } else if (definition instanceof EnumTypeDefinition) {
            return new GraphQLEnumTypeCompositeDefinition();
        } else if (definition instanceof ScalarTypeDefinition) {
            return new GraphQLScalarTypeCompositeDefinition();
        } else if (definition instanceof DirectiveDefinition) {
            return new GraphQLDirectiveTypeCompositeDefinition();
        } else if (definition instanceof SchemaDefinition) {
            return new GraphQLSchemaTypeCompositeDefinition();
        } else {
            throw new IllegalStateException("Unknown definition type: " + definition.getClass().getName());
        }
    }

    @Nullable
    public GraphQLCompositeDefinition<?> getCompositeDefinition(@NotNull SDLDefinition<?> definition) {
        if (definition instanceof SchemaDefinition) {
            return mySchemaCompositeDefinition;
        }

        if (!(definition instanceof NamedNode)) {
            return null;
        }

        return myNamedCompositeDefinitions.computeIfAbsent(
            ((NamedNode<?>) definition).getName(), name -> createCompositeDefinition(definition));
    }

    public void addTypeDefinition(@NotNull SDLDefinition<?> definition) {
        GraphQLCompositeDefinition<?> builder = getCompositeDefinition(definition);

        if (builder == null) {
            LOG.warn("No suitable builder for " + definition.getClass().getName());
            return;
        }

        if (builder instanceof GraphQLDirectiveTypeCompositeDefinition) {
            ((GraphQLDirectiveTypeCompositeDefinition) builder).addDefinition(ObjectUtils.tryCast(definition, DirectiveDefinition.class));
        } else if (builder instanceof GraphQLEnumTypeCompositeDefinition) {
            ((GraphQLEnumTypeCompositeDefinition) builder).addDefinition(ObjectUtils.tryCast(definition, EnumTypeDefinition.class));
        } else if (builder instanceof GraphQLInputObjectTypeCompositeDefinition) {
            ((GraphQLInputObjectTypeCompositeDefinition) builder).addDefinition(ObjectUtils.tryCast(definition, InputObjectTypeDefinition.class));
        } else if (builder instanceof GraphQLInterfaceTypeCompositeDefinition) {
            ((GraphQLInterfaceTypeCompositeDefinition) builder).addDefinition(ObjectUtils.tryCast(definition, InterfaceTypeDefinition.class));
        } else if (builder instanceof GraphQLObjectTypeCompositeDefinition) {
            ((GraphQLObjectTypeCompositeDefinition) builder).addDefinition(ObjectUtils.tryCast(definition, ObjectTypeDefinition.class));
        } else if (builder instanceof GraphQLScalarTypeCompositeDefinition) {
            ((GraphQLScalarTypeCompositeDefinition) builder).addDefinition(ObjectUtils.tryCast(definition, ScalarTypeDefinition.class));
        } else if (builder instanceof GraphQLSchemaTypeCompositeDefinition) {
            ((GraphQLSchemaTypeCompositeDefinition) builder).addDefinition(ObjectUtils.tryCast(definition, SchemaDefinition.class));
        } else if (builder instanceof GraphQLUnionTypeCompositeDefinition) {
            ((GraphQLUnionTypeCompositeDefinition) builder).addDefinition(ObjectUtils.tryCast(definition, UnionTypeDefinition.class));
        } else {
            LOG.error("Unknown builder type: " + builder.getClass().getName());
        }
    }

    public void addExtensionDefinition(@NotNull SDLDefinition<?> definition) {
        LOG.assertTrue(GraphQLSchemaUtil.isExtension(definition));

        GraphQLCompositeDefinition<?> builder = getCompositeDefinition(definition);

        if (builder == null) {
            LOG.warn("No suitable builder for extension definition " + definition.getClass().getName());
            return;
        }

        if (!(builder instanceof GraphQLExtendableCompositeDefinition)) {
            return;
        }

        if (builder instanceof GraphQLEnumTypeCompositeDefinition) {
            ((GraphQLEnumTypeCompositeDefinition) builder).addExtension(ObjectUtils.tryCast(definition, EnumTypeExtensionDefinition.class));
        } else if (builder instanceof GraphQLInputObjectTypeCompositeDefinition) {
            ((GraphQLInputObjectTypeCompositeDefinition) builder).addExtension(ObjectUtils.tryCast(definition, InputObjectTypeExtensionDefinition.class));
        } else if (builder instanceof GraphQLInterfaceTypeCompositeDefinition) {
            ((GraphQLInterfaceTypeCompositeDefinition) builder).addExtension(ObjectUtils.tryCast(definition, InterfaceTypeExtensionDefinition.class));
        } else if (builder instanceof GraphQLObjectTypeCompositeDefinition) {
            ((GraphQLObjectTypeCompositeDefinition) builder).addExtension(ObjectUtils.tryCast(definition, ObjectTypeExtensionDefinition.class));
        } else if (builder instanceof GraphQLScalarTypeCompositeDefinition) {
            ((GraphQLScalarTypeCompositeDefinition) builder).addExtension(ObjectUtils.tryCast(definition, ScalarTypeExtensionDefinition.class));
        } else if (builder instanceof GraphQLSchemaTypeCompositeDefinition) {
            ((GraphQLSchemaTypeCompositeDefinition) builder).addExtension(ObjectUtils.tryCast(definition, SchemaExtensionDefinition.class));
        } else if (builder instanceof GraphQLUnionTypeCompositeDefinition) {
            ((GraphQLUnionTypeCompositeDefinition) builder).addExtension(ObjectUtils.tryCast(definition, UnionTypeExtensionDefinition.class));
        } else {
            LOG.error("Unknown extension builder type: " + builder.getClass().getName());
        }
    }

    public void addDefinition(@NotNull SDLDefinition<?> definition) {
        if (GraphQLSchemaUtil.isExtension(definition)) {
            addExtensionDefinition(definition);
        } else {
            addTypeDefinition(definition);
        }
    }

    @SuppressWarnings("rawtypes")
    public void addFromDocument(@NotNull Document document) {
        List<Definition> definitions = document.getDefinitions();
        for (Definition definition : definitions) {
            if (definition instanceof SDLDefinition) {
                addDefinition(((SDLDefinition<?>) definition));
            }
        }
    }

    @NotNull
    public TypeDefinitionRegistry buildTypeDefinitionRegistry() {
        TypeDefinitionRegistry registry = new TypeDefinitionRegistry();

        SchemaDefinition schemaDefinition = mySchemaCompositeDefinition.getMergedDefinition();
        if (schemaDefinition != null) {
            registry.add(schemaDefinition);
        }
        mySchemaCompositeDefinition.getBuiltExtensions().forEach(registry::add);

        myNamedCompositeDefinitions.values().forEach(builder -> {
            SDLDefinition<?> definition = builder.getMergedDefinition();
            if (definition != null) {
                registry.add(definition);
            }

            if (builder instanceof GraphQLExtendableCompositeDefinition) {
                ((GraphQLExtendableCompositeDefinition<?, ?>) builder).getBuiltExtensions()
                    .forEach(registry::add);
            }
        });

        // TODO: [intellij] validate source definitions explicitly

        return registry;
    }
}
