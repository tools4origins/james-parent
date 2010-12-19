/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.container.spring.lifecycle.api;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

/**
 * Abstract base class which BeanPostProcessors should extend if they provide an
 * LifeCycle handling
 * 
 * 
 * @param <T>
 */
public abstract class AbstractLifeCycleBeanPostProcessor<T> implements BeanPostProcessor, PriorityOrdered, BeanFactoryAware {

    private int order = Ordered.HIGHEST_PRECEDENCE;
    private ListableBeanFactory factory;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.config.BeanPostProcessor#
     * postProcessAfterInitialization(java.lang.Object, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public final Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
        try {
            Class<T> lClass = getLifeCycleInterface();
            if (lClass.isInstance(bean))
                // check if the bean is registered in the context. If not it was create by the InstanceFactory and so there is no need to execute the callback
                if (factory.containsBeanDefinition(name)) {
                    executeLifecycleMethodAfterInit((T) bean, name);
                }
        } catch (Exception e) {
            throw new FatalBeanException("Unable to execute lifecycle method on bean" + name, e);
        }
        return bean;
    }

    /**
     * Return the class which mark the lifecycle
     * 
     * @return interfaceClass
     */
    protected abstract Class<T> getLifeCycleInterface();

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.config.BeanPostProcessor#
     * postProcessBeforeInitialization(java.lang.Object, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public final Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
        try {
            Class<T> lClass = getLifeCycleInterface();
            if (lClass.isInstance(bean))
                // check if the bean is registered in the context. If not it was create by the InstanceFactory and so there is no need to execute the callback
                if (factory.containsBeanDefinition(name)) {
                    executeLifecycleMethodBeforeInit((T) bean, name);
                }
        } catch (Exception e) {
            throw new FatalBeanException("Unable to execute lifecycle method on bean" + name, e);
        }
        return bean;
    }

    /**
     * Method which gets executed if the bean implement the LifeCycleInterface.
     * Override this if you wish perform any action. Default is todo nothing
     * 
     * @param bean
     *            the actual bean
     * @param beanname
     *            then name of the bean
     * @throws Exception
     */
    protected void executeLifecycleMethodBeforeInit(T bean, String beanname) throws Exception {

    }

    /**
     * Method which gets executed if the bean implement the LifeCycleInterface.
     * Override this if you wish perform any action. Default is todo nothing
     * 
     * @param bean
     *            the actual bean
     * @param beanname
     *            then name of the bean
     * @param componentName
     *            the component name
     * @throws Exception
     */
    protected void executeLifecycleMethodAfterInit(T bean, String beanname) throws Exception {

    }

    public void setOrder(int order) {
        this.order = order;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.core.Ordered#getOrder()
     */
    public int getOrder() {
        return order;
    }
    

    /*
     * (non-Javadoc)
     * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
     */
    public void setBeanFactory(BeanFactory factory) throws BeansException {
        this.factory = (ListableBeanFactory) factory;
    }


}
