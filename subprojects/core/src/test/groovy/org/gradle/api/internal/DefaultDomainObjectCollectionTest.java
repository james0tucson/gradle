/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal;

import org.gradle.api.Action;
import org.gradle.api.DomainObjectCollection;
import org.gradle.api.specs.Spec;
import org.gradle.util.HelperUtil;
import org.gradle.util.TestClosure;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import static org.gradle.util.WrapUtil.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(JMock.class)
public class DefaultDomainObjectCollectionTest {
    private final JUnit4Mockery context = new JUnit4Mockery();
    private final DefaultDomainObjectCollection<CharSequence> container = new DefaultDomainObjectCollection<CharSequence>(CharSequence.class, new LinkedHashSet<CharSequence>());

    @Test
    public void canGetAllDomainObjectsForEmptyCollection() {
        assertTrue(container.isEmpty());
    }

    @Test
    public void canGetAllDomainObjectsOrderedByOrderAdded() {
        container.add("b");
        container.add("a");
        container.add("c");

        assertThat(toList(container), equalTo(toList((CharSequence) "b", "a", "c")));
    }

    @Test
    public void canIterateOverEmptyCollection() {
        Iterator<CharSequence> iterator = container.iterator();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void canIterateOverDomainObjectsOrderedByOrderAdded() {
        container.add("b");
        container.add("a");
        container.add("c");

        Iterator<CharSequence> iterator = container.iterator();
        assertThat(iterator.next(), equalTo((CharSequence) "b"));
        assertThat(iterator.next(), equalTo((CharSequence) "a"));
        assertThat(iterator.next(), equalTo((CharSequence) "c"));
        assertFalse(iterator.hasNext());
    }

    @Test
    public void canGetAllMatchingDomainObjectsOrderedByOrderAdded() {
        Spec<CharSequence> spec = new Spec<CharSequence>() {
            public boolean isSatisfiedBy(CharSequence element) {
                return !element.equals("b");
            }
        };

        container.add("a");
        container.add("b");
        container.add("c");

        assertThat(toList(container.findAll(spec)), equalTo(toList((CharSequence) "a", "c")));
    }

    @Test
    public void getAllMatchingDomainObjectsReturnsEmptySetWhenNoMatches() {
        Spec<CharSequence> spec = new Spec<CharSequence>() {
            public boolean isSatisfiedBy(CharSequence element) {
                return false;
            }
        };

        container.add("a");

        assertTrue(container.findAll(spec).isEmpty());
    }

    @Test
    public void canGetFilteredCollectionContainingAllObjectsWhichMeetSpec() {
        Spec<CharSequence> spec = new Spec<CharSequence>() {
            public boolean isSatisfiedBy(CharSequence element) {
                return !element.equals("b");
            }
        };
        TestClosure testClosure = new TestClosure() {
            public Object call(Object param) {
                return !param.equals("b");    
            }
        };

        container.add("a");
        container.add("b");
        container.add("c");

        assertThat(toList(container.matching(spec)), equalTo(toList((CharSequence) "a", "c")));
        assertThat(toList(container.matching(HelperUtil.toClosure(testClosure))), equalTo(toList((CharSequence) "a", "c")));
    }

    @Test
    public void canGetFilteredCollectionContainingAllObjectsWhichHaveType() {
        container.add("c");
        container.add("a");
        container.add(new StringBuffer("b"));

        assertThat(toList(container.withType(CharSequence.class)), equalTo(toList(container)));
        assertThat(toList(container.withType(String.class)), equalTo(toList("c", "a")));
    }

    // @Test
    // public void canExecuteActionForAllElementsInATypeFilteredCollection() {
    //     final Action<CharSequence> action = context.mock(Action.class);
    // 
    //     container.add("c");
    //     container.add(new StringBuffer("b"));
    // 
    //     context.checking(new Expectations(){{
    //         one(action).execute("c");
    //         one(action).execute("a");
    //     }});
    // 
    //     container.withType(String.class, action);
    //     container.add("a");
    // }

    @Test
    public void canExecuteClosureForAllElementsInATypeFilteredCollection() {
        final TestClosure closure = context.mock(TestClosure.class);

        container.add("c");
        container.add(new StringBuffer("b"));

        context.checking(new Expectations(){{
            one(closure).call("c");
            one(closure).call("a");
        }});
        
        container.withType(String.class, HelperUtil.toClosure(closure));
        container.add("a");
    }

    @Test
    public void filteredCollectionIsLive() {
        Spec<CharSequence> spec = new Spec<CharSequence>() {
            public boolean isSatisfiedBy(CharSequence element) {
                return !element.equals("a");
            }
        };

        container.add("a");

        DomainObjectCollection<CharSequence> filteredCollection = container.matching(spec);
        assertTrue(filteredCollection.isEmpty());

        container.add("b");
        container.add("c");

        assertThat(toList(filteredCollection), equalTo(toList((CharSequence) "b", "c")));
    }

    @Test
    public void filteredCollectionExecutesActionWhenMatchingObjectAdded() {
        final Action<CharSequence> action = context.mock(Action.class);

        context.checking(new Expectations() {{
            one(action).execute("a");
        }});

        Spec<CharSequence> spec = new Spec<CharSequence>() {
            public boolean isSatisfiedBy(CharSequence element) {
                return !element.equals("b");
            }
        };

        container.matching(spec).whenObjectAdded(action);

        container.add("a");
        container.add("b");
    }

    @Test
    public void filteredCollectionExecutesClosureWhenMatchingObjectAdded() {
        final TestClosure closure = context.mock(TestClosure.class);

        context.checking(new Expectations() {{
            one(closure).call("a");
        }});

        Spec<CharSequence> spec = new Spec<CharSequence>() {
            public boolean isSatisfiedBy(CharSequence element) {
                return !element.equals("b");
            }
        };

        container.matching(spec).whenObjectAdded(HelperUtil.toClosure(closure));

        container.add("a");
        container.add("b");
    }

    @Test
    public void canChainFilteredCollections() {
        Spec<CharSequence> spec = new Spec<CharSequence>() {
            public boolean isSatisfiedBy(CharSequence element) {
                return !element.equals("b");
            }
        };
        Spec<String> spec2 = new Spec<String>() {
            public boolean isSatisfiedBy(String element) {
                return !element.equals("c");
            }
        };

        container.add("a");
        container.add("b");
        container.add("c");
        container.add(new StringBuffer("d"));

        DomainObjectCollection<String> collection = container.matching(spec).withType(String.class).matching(spec2);
        assertThat(toList(collection), equalTo(toList("a")));
    }

    @Test
    public void findAllRetainsIterationOrder() {
        container.add("a");
        container.add("b");
        container.add("c");

        Collection<CharSequence> collection = container.findAll(HelperUtil.toClosure("{ it != 'b' }"));
        assertThat(collection, instanceOf(List.class));
        assertThat(collection, equalTo((Collection) toList("a", "c")));
    }

    @Test
    public void findAllDoesNotReturnALiveCollection() {
        container.add("a");
        container.add("b");
        container.add("c");

        Collection<CharSequence> collection = container.findAll(HelperUtil.toClosure("{ it != 'b' }"));

        container.add("d");
        assertThat(collection, equalTo((Collection) toList("a", "c")));
    }
    
    @Test
    public void callsActionWhenObjectAdded() {
        final Action<CharSequence> action = context.mock(Action.class);

        context.checking(new Expectations() {{
            one(action).execute("a");
        }});

        container.whenObjectAdded(action);
        container.add("a");
    }

    @Test
    public void callsClosureWithNewObjectAsParameterWhenObjectAdded() {
        final TestClosure closure = context.mock(TestClosure.class);

        context.checking(new Expectations() {{
            one(closure).call("a");
        }});

        container.whenObjectAdded(HelperUtil.toClosure(closure));
        container.add("a");
    }

    @Test
    public void callsClosureWithRemovedObjectAsParameterWhenObjectRemoved() {
        final TestClosure closure = context.mock(TestClosure.class);

        container.add("a");

        context.checking(new Expectations() {{
            one(closure).call("a");
        }});

        container.whenObjectRemoved(HelperUtil.toClosure(closure));
        container.remove("a");
    }

    @Test
    public void callsClosureWithNewObjectAsDelegateWhenObjectAdded() {
        container.whenObjectAdded(HelperUtil.toClosure("{ assert delegate == 'a' }"));
        container.add("a");
    }

    
    /*
        Commented out because there is no longer an implicit replace due to domain object collections
        now implementing the semantics of collection, which is incompatible with this. - LD.
    */
    // @Test
    // public void callsRemoveActionWhenObjectReplaced() {
    //     final Action<CharSequence> action = context.mock(Action.class);
    //     final String original = "a";
    // 
    //     context.checking(new Expectations() {{
    //         one(action).execute(with(sameInstance(original)));
    //     }});
    // 
    //     container.whenObjectRemoved(action);
    //     container.add(original);
    //     container.add("a");
    // }

    @Test
    public void callsRemoveActionWhenObjectRemoved() {
        final Action<CharSequence> action = context.mock(Action.class);
        final String original = "a";

        context.checking(new Expectations() {{
            one(action).execute(with(sameInstance(original)));
        }});

        container.whenObjectRemoved(action);
        container.add(original);
        assertTrue(container.remove(original));
    }

    @Test
    public void allCallsActionForEachExistingObject() {
        final Action<CharSequence> action = context.mock(Action.class);

        context.checking(new Expectations() {{
            one(action).execute("a");
        }});

        container.add("a");
        container.all(action);
    }

    @Test
    public void allCallsClosureForEachExistingObject() {
        final TestClosure closure = context.mock(TestClosure.class);

        context.checking(new Expectations() {{
            one(closure).call("a");
        }});

        container.add("a");
        container.all(HelperUtil.toClosure(closure));
    }

    @Test
    public void allCallsActionForEachNewObject() {
        final Action<CharSequence> action = context.mock(Action.class);

        context.checking(new Expectations() {{
            one(action).execute("a");
        }});

        container.all(action);
        container.add("a");
    }

    @Test
    public void allCallsClosureForEachNewObject() {
        final TestClosure closure = context.mock(TestClosure.class);

        context.checking(new Expectations() {{
            one(closure).call("a");
        }});

        container.all(HelperUtil.toClosure(closure));
        container.add("a");
    }

    @Test
    public void allCallsClosureWithObjectAsDelegate() {
        container.all(HelperUtil.toClosure(" { assert delegate == 'a' } "));
        container.add("a");
    }

    @Test
    public void allCallsActionForEachNewObjectAddedByTheAction() {
        final Action<CharSequence> action = context.mock(Action.class);

        context.checking(new Expectations() {{
            one(action).execute("a");
            will(new org.jmock.api.Action() {
                public Object invoke(Invocation invocation) throws Throwable {
                    container.add("c");
                    return null;
                }

                public void describeTo(Description description) {
                    description.appendText("add 'c'");
                }
            });
            one(action).execute("b");
            one(action).execute("c");
        }});

        container.add("a");
        container.add("b");
        container.all(action);
    }

    @Test
    public void callsVetoActionBeforeObjectIsAdded() {
        final Runnable action = context.mock(Runnable.class);
        container.beforeChange(action);

        context.checking(new Expectations() {{
            one(action).run();
        }});

        container.add("a");
    }

    @Test
    public void objectIsNotAddedWhenVetoActionThrowsAnException() {
        final Runnable action = context.mock(Runnable.class);
        final RuntimeException failure = new RuntimeException();
        container.beforeChange(action);

        context.checking(new Expectations() {{
            one(action).run();
            will(throwException(failure));
        }});

        try {
            container.add("a");
            fail();
        } catch (RuntimeException e) {
            assertThat(e, sameInstance(failure));
        }

        assertThat(container, not(hasItem((CharSequence) "a")));
    }

    @Test
    public void callsVetoActionOnceBeforeCollectionIsAdded() {
        final Runnable action = context.mock(Runnable.class);
        container.beforeChange(action);

        context.checking(new Expectations() {{
            one(action).run();
        }});

        container.addAll(toList("a", "b"));
    }

    @Test
    public void callsVetoActionBeforeObjectIsRemoved() {
        final Runnable action = context.mock(Runnable.class);
        container.beforeChange(action);

        context.checking(new Expectations() {{
            one(action).run();
        }});

        container.remove("a");
    }

    @Test
    public void objectIsNotRemovedWhenVetoActionThrowsAnException() {
        final Runnable action = context.mock(Runnable.class);
        final RuntimeException failure = new RuntimeException();
        container.add("a");
        container.beforeChange(action);
        
        context.checking(new Expectations() {{
            one(action).run();
            will(throwException(failure));
        }});

        try {
            container.remove("a");
            fail();
        } catch (RuntimeException e) {
            assertThat(e, sameInstance(failure));
        }

        assertThat(container, hasItem((CharSequence) "a"));
    }

    @Test
    public void callsVetoActionBeforeCollectionIsCleared() {
        final Runnable action = context.mock(Runnable.class);
        container.beforeChange(action);

        context.checking(new Expectations() {{
            one(action).run();
        }});

        container.clear();
    }

    @Test
    public void callsVetoActionOnceBeforeCollectionIsRemoved() {
        final Runnable action = context.mock(Runnable.class);
        container.beforeChange(action);

        context.checking(new Expectations() {{
            one(action).run();
        }});

        container.removeAll(toList("a", "b"));
    }

    @Test
    public void callsVetoActionOnceBeforeCollectionIsIntersected() {
        final Runnable action = context.mock(Runnable.class);
        container.add("a");
        container.add("b");
        container.beforeChange(action);

        context.checking(new Expectations() {{
            one(action).run();
        }});

        container.retainAll(toList());
    }

    @Test
    public void canRemoveAndMaintainOrder() {
        container.add("b");
        container.add("a");
        container.add("c");

        assertTrue(container.remove("a"));
        assertThat(toList(container), equalTo(toList((CharSequence) "b", "c")));
    }

    @Test
    public void canRemoveNonExistentObject() {
        assertFalse(container.remove("a"));
    }
    
}
