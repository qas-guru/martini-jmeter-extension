/*
Copyright 2018 Penny Rohr Curich

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package guru.qas.martini.jmeter.spring.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import guru.qas.martini.jmeter.controller.DefaultMartiniScopeControllerBean;
import guru.qas.martini.jmeter.controller.MartiniScopeControllerBean;

@Configuration
@Lazy
public class MartiniScopeControllerBeanConfiguration {

	@Bean
	MartiniScopeControllerBean getMartiniScopeControllerBean(
		AutowireCapableBeanFactory beanFactory,
		@Value("${martini.scope.controller.bean.impl:#{null}}")
			Class<? extends MartiniScopeControllerBean> implementation
	) {
		implementation = null == implementation ? DefaultMartiniScopeControllerBean.class : implementation;
		return beanFactory.createBean(implementation);
	}
}
