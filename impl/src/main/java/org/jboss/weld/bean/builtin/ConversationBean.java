package org.jboss.weld.bean.builtin;

import java.lang.annotation.Annotation;
import java.util.Locale;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.weld.bootstrap.BeanDeployerEnvironment;
import org.jboss.weld.context.ConversationContext;
import org.jboss.weld.context.conversation.ConversationImpl;
import org.jboss.weld.manager.BeanManagerImpl;

public class ConversationBean extends AbstractStaticallyDecorableBuiltInBean<Conversation> {

    private Instance<ConversationContext> conversationContexts;

    public ConversationBean(BeanManagerImpl beanManager) {
        super(beanManager, Conversation.class);
    }

    @Override
    public void internalInitialize(BeanDeployerEnvironment environment) {
        super.internalInitialize(environment);
        this.conversationContexts = getBeanManager().instance().select(ConversationContext.class);
    }

    @Override
    protected Conversation newInstance(InjectionPoint ip, CreationalContext<Conversation> creationalContext) {
        for (ConversationContext conversationContext : getBeanManager().instance().select(ConversationContext.class)) {
            if (conversationContext.isActive()) {
                return conversationContext.getCurrentConversation();
            }
        }
        /*
        * Can't get a "real" Conversation, but we need to return something, so
        * return this dummy Conversation which will simply throw a
        * ContextNotActiveException for every method call as the spec requires.
        */
        return new ConversationImpl(conversationContexts);
    }

    public void destroy(Conversation instance, CreationalContext<Conversation> creationalContext) {
    }

    @Override
    public Class<?> getBeanClass() {
        return ConversationImpl.class;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return RequestScoped.class;
    }

    @Override
    public String getName() {
        return Conversation.class.getName().toLowerCase(Locale.ENGLISH);
    }

}
