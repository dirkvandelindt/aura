/*
 * Copyright (C) 2013 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.auraframework.impl.css.style;

import com.google.common.collect.ImmutableList;
import com.salesforce.omakase.plugin.Plugin;
import org.auraframework.Aura;
import org.auraframework.adapter.StyleAdapter;
import org.auraframework.builder.BaseStyleDefBuilder;
import org.auraframework.css.ResolveStrategy;
import org.auraframework.css.TokenValueProvider;
import org.auraframework.def.BaseStyleDef;
import org.auraframework.def.TokenDef;
import org.auraframework.expression.Expression;
import org.auraframework.expression.PropertyReference;
import org.auraframework.impl.DefinitionAccessImpl;
import org.auraframework.impl.css.parser.CssPreprocessor;
import org.auraframework.impl.expression.AuraExpressionBuilder;
import org.auraframework.impl.system.DefinitionImpl;
import org.auraframework.impl.util.AuraUtil;
import org.auraframework.system.AuraContext;
import org.auraframework.system.MasterDefRegistry;
import org.auraframework.throwable.AuraRuntimeException;
import org.auraframework.throwable.quickfix.AuraValidationException;
import org.auraframework.throwable.quickfix.QuickFixException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for concrete {@link BaseStyleDef} implementations.
 */
public abstract class AbstractStyleDef<D extends BaseStyleDef> extends DefinitionImpl<D> implements BaseStyleDef {
    private static final long serialVersionUID = -7239904547091800250L;

    private final String content;
    private final Set<String> expressions;

    protected AbstractStyleDef(Builder<D> builder) {
        super(builder);
        this.content = builder.content;
        this.expressions = AuraUtil.immutableSet(builder.expressions);
    }

    @Override
    public String getCode() {
        return getCode(ImmutableList.<Plugin>of());
    }

    @Override
    public String getCode(List<Plugin> plugins) {
        try {
            return CssPreprocessor.runtime()
                    .source(content)
                    .resourceName(descriptor.getQualifiedName())
                    .tokens(descriptor)
                    .extras(plugins)
                    .parse()
                    .content();
        } catch (QuickFixException e) {
            throw new AuraRuntimeException(e);
        }
    }

    @Override
    public String getRawCode() {
        return content;
    }

    @Override
    public Set<String> getExpressions() {
        return expressions;
    }

    @Override
    public Set<String> getTokenNames() throws AuraValidationException {
        Set<String> set = new HashSet<>();

        if (!expressions.isEmpty()) {
            Set<PropertyReference> tmp = new HashSet<>();
            for (String rawExpression : expressions) {
                Expression expression = AuraExpressionBuilder.INSTANCE.buildExpression(rawExpression, null);
                expression.gatherPropertyReferences(tmp);
            }

            for (PropertyReference propRef : tmp) {
                set.add(propRef.getRoot());
            }
        }

        return set;
    }

    @Override
    public void validateReferences() throws QuickFixException {
        super.validateReferences();

        // validate tokens
        if (!expressions.isEmpty()) {
            StyleAdapter adapter = Aura.getStyleAdapter();
            TokenValueProvider vp = adapter.getTokenValueProvider(descriptor, ResolveStrategy.RESOLVE_DEFAULTS);
            MasterDefRegistry mdr = Aura.getDefinitionService().getDefRegistry();

            for (String reference : expressions) {
                // getValue will validate it's a valid expression/variable
                vp.getValue(reference, getLocation());

                // access checks FIXME this probably isn't the right location for this, and this check
                // can be bypassed via cross references, among other issues.
                for (List<TokenDef> tokenDefs : vp.extractTokenDefs(reference)) {
                    for (TokenDef tokenDef : tokenDefs) {
                        mdr.assertAccess(descriptor, tokenDef);
                    }
                }
            }
        }
    }

    /** common builder for style defs */
    public static abstract class Builder<D extends BaseStyleDef> extends BuilderImpl<D> implements BaseStyleDefBuilder<D> {
        private String content;
        private Set<String> expressions;

        public Builder(Class<D> defClass) {
            super(defClass);
            setAccess(new DefinitionAccessImpl(AuraContext.Access.PUBLIC));
        }

        @Override
        public Builder<D> setContent(String content) {
            this.content = content;
            return this;
        }

        @Override
        public Builder<D> setTokenExpressions(Set<String> expressions) {
            this.expressions = expressions;
            return this;
        }
    }
}
