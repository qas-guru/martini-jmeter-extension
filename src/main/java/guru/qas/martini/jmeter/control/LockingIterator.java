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

package guru.qas.martini.jmeter.control;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import javax.annotation.Nonnull;

import com.google.common.collect.ForwardingIterator;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class LockingIterator<T> extends ForwardingIterator<T> implements Lock {

	protected final Iterator<T> iterator;
	protected final Lock lock;

	public LockingIterator(@Nonnull Iterator<T> iterator, @Nonnull Lock lock) {
		this.iterator = checkNotNull(iterator, "null Iterator");
		this.lock = checkNotNull(lock, "null Lock");
	}

	@Override
	protected Iterator<T> delegate() {
		return iterator;
	}

	@Override
	public void lock() {
		lock.lock();
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		lock.lockInterruptibly();
	}

	@Override
	public boolean tryLock() {
		return lock.tryLock();
	}

	@Override
	public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
		return lock.tryLock(time, unit);
	}

	@Override
	public void unlock() {
		lock.unlock();
	}

	@Nonnull
	@Override
	public Condition newCondition() {
		return lock.newCondition();
	}
}
