/*
Copyright 2017 Penny Rohr Curich

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

package qas.guru.martini.jmeter.control;

import java.io.Serializable;
import java.util.Collection;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.control.ReplaceableController;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.collections.HashTree;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import guru.qas.martini.Martini;
import guru.qas.martini.Mixologist;

@SuppressWarnings("WeakerAccess")
public class MartiniController extends GenericController implements Serializable/*, ReplaceableController*/ {

	public MartiniController() {
		super();
	}

	@Override
	public void initialize() {
		super.initialize();
		JMeterContext threadContext = super.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		initialize(variables);
	}

	protected void initialize(JMeterVariables variables) {
		Object o = variables.getObject("applicationContext");
		ClassPathXmlApplicationContext context = ClassPathXmlApplicationContext.class.cast(o);
		Mixologist mixologist = context.getBean(Mixologist.class);
		initialize(mixologist);
	}

	protected void initialize(Mixologist mixologist) {
		Collection<Martini> martinis = mixologist.getMartinis();
		if (martinis.isEmpty()) {
			// Shoot up some kind of warning message.
		}
		for (Martini martini : martinis) {
			System.out.println("martini: " + martini);
		}
		//super.addTestElement();
	}

//	@Override
//	public HashTree getReplacementSubTree() {
//
//		/*
//		        HashTree tree = new ListedHashTree();
//        if (selectedNode != null) {
//            // Use a local variable to avoid replacing reference by modified clone (see Bug 54950)
//            JMeterTreeNode nodeToReplace = selectedNode;
//            // We clone to avoid enabling existing node
//            if (!nodeToReplace.isEnabled()) {
//                nodeToReplace = cloneTreeNode(selectedNode);
//                nodeToReplace.setEnabled(true);
//            }
//            HashTree subtree = tree.add(nodeToReplace);
//            createSubTree(subtree, nodeToReplace);
//        }
//        return tree;
//		 */
//	}
//
//	@Override
//	public void resolveReplacementSubTree(JMeterTreeNode context) {
//	}
}
