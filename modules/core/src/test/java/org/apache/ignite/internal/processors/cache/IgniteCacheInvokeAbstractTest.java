/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.apache.ignite.internal.processors.cache;

import org.apache.ignite.*;
import org.apache.ignite.lang.*;
import org.apache.ignite.transactions.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.jetbrains.annotations.*;

import javax.cache.processor.*;

import java.util.*;
import java.util.concurrent.*;

import static org.apache.ignite.transactions.IgniteTxConcurrency.*;
import static org.apache.ignite.transactions.IgniteTxIsolation.*;
import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheFlag.*;
import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 *
 */
public abstract class IgniteCacheInvokeAbstractTest extends IgniteCacheAbstractTest {
    /** */
    private Integer lastKey = 0;

    /**
     * @throws Exception If failed.
     */
    public void testInvoke() throws Exception {
        IgniteCache<Integer, Integer> cache = jcache();

        invoke(cache, null);

        if (atomicityMode() == TRANSACTIONAL) {
            invoke(cache, PESSIMISTIC);

            invoke(cache, OPTIMISTIC);
        }
        else if (gridCount() > 1) {
            cache = cache.flagsOn(FORCE_TRANSFORM_BACKUP);

            invoke(cache, null);
        }
    }

    /**
     * @param cache Cache.
     * @param txMode Not null transaction concurrency mode if explicit transaction should be started.
     * @throws Exception If failed.
     */
    private void invoke(final IgniteCache<Integer, Integer> cache, @Nullable IgniteTxConcurrency txMode)
        throws Exception {
        IncrementProcessor incProcessor = new IncrementProcessor();

        for (final Integer key : keys()) {
            log.info("Test invoke [key=" + key + ", txMode=" + txMode + ']');

            cache.remove(key);

            IgniteTx tx = startTx(txMode);

            Integer res = cache.invoke(key, incProcessor);

            if (tx != null)
                tx.commit();

            assertEquals(-1, (int)res);

            checkValue(key, 1);

            tx = startTx(txMode);

            res = cache.invoke(key, incProcessor);

            if (tx != null)
                tx.commit();

            assertEquals(1, (int)res);

            checkValue(key, 2);

            tx = startTx(txMode);

            res = cache.invoke(key, incProcessor);

            if (tx != null)
                tx.commit();

            assertEquals(2, (int)res);

            checkValue(key, 3);

            tx = startTx(txMode);

            res = cache.invoke(key, new ArgumentsSumProcessor(), 10, 20, 30);

            if (tx != null)
                tx.commit();

            assertEquals(3, (int)res);

            checkValue(key, 63);

            tx = startTx(txMode);

            String strRes = cache.invoke(key, new ToStringProcessor());

            if (tx != null)
                tx.commit();

            assertEquals("63", strRes);

            checkValue(key, 63);

            tx = startTx(txMode);

            GridTestUtils.assertThrows(log, new Callable<Void>() {
                @Override public Void call() throws Exception {
                    cache.invoke(key, new ExceptionProcessor(63));

                    return null;
                }
            }, EntryProcessorException.class, "Test processor exception.");

            if (tx != null)
                tx.commit();

            checkValue(key, 63);

            IgniteCache<Integer, Integer> asyncCache = cache.enableAsync();

            assertTrue(asyncCache.isAsync());

            assertNull(asyncCache.invoke(key, incProcessor));

            IgniteFuture<Integer> fut = asyncCache.future();

            assertNotNull(fut);

            assertEquals(63, (int)fut.get());

            checkValue(key, 64);

            tx = startTx(txMode);

            assertNull(cache.invoke(key, new RemoveProcessor(64)));

            if (tx != null)
                tx.commit();

            checkValue(key, null);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testInvokeAll() throws Exception {
        IgniteCache<Integer, Integer> cache = jcache();

        invokeAll(cache, null);

        if (atomicityMode() == TRANSACTIONAL) {
            invokeAll(cache, PESSIMISTIC);

            invokeAll(cache, OPTIMISTIC);
        }
        else if (gridCount() > 1) {
            cache = cache.flagsOn(FORCE_TRANSFORM_BACKUP);

            invokeAll(cache, null);
        }
    }

    /**
     * @param cache Cache.
     * @param txMode Not null transaction concurrency mode if explicit transaction should be started.
     * @throws Exception If failed.
     */
    private void invokeAll(IgniteCache<Integer, Integer> cache, @Nullable IgniteTxConcurrency txMode) throws Exception {
        invokeAll(cache, new HashSet<>(primaryKeys(cache, 3, 0)), txMode);

        if (gridCount() > 1) {
            invokeAll(cache, new HashSet<>(backupKeys(cache, 3, 0)), txMode);

            invokeAll(cache, new HashSet<>(nearKeys(cache, 3, 0)), txMode);

            Set<Integer> keys = new HashSet<>();

            keys.addAll(primaryKeys(jcache(0), 3, 0));
            keys.addAll(primaryKeys(jcache(1), 3, 0));
            keys.addAll(primaryKeys(jcache(2), 3, 0));

            invokeAll(cache, keys, txMode);
        }

        Set<Integer> keys = new HashSet<>();

        for (int i = 0; i < 1000; i++)
            keys.add(i);

        invokeAll(cache, keys, txMode);
    }

    /**
     * @param cache Cache.
     * @param keys Keys.
     * @param txMode Not null transaction concurrency mode if explicit transaction should be started.
     * @throws Exception If failed.
     */
    private void invokeAll(IgniteCache<Integer, Integer> cache,
        Set<Integer> keys,
        @Nullable IgniteTxConcurrency txMode) throws Exception {
        cache.removeAll(keys);

        log.info("Test invokeAll [keys=" + keys + ", txMode=" + txMode + ']');

        IncrementProcessor incProcessor = new IncrementProcessor();

        IgniteTx tx = startTx(txMode);

        Map<Integer, EntryProcessorResult<Integer>> resMap = cache.invokeAll(keys, incProcessor);

        if (tx != null)
            tx.commit();

        Map<Object, Object> exp = new HashMap<>();

        for (Integer key : keys)
            exp.put(key, -1);

        checkResult(resMap, exp);

        for (Integer key : keys)
            checkValue(key, 1);

        tx = startTx(txMode);

        resMap = cache.invokeAll(keys, incProcessor);

        if (tx != null)
            tx.commit();

        exp = new HashMap<>();

        for (Integer key : keys)
            exp.put(key, 1);

        checkResult(resMap, exp);

        for (Integer key : keys)
            checkValue(key, 2);

        tx = startTx(txMode);

        resMap = cache.invokeAll(keys, new ArgumentsSumProcessor(), 10, 20, 30);

        if (tx != null)
            tx.commit();

        for (Integer key : keys)
            exp.put(key, 3);

        checkResult(resMap, exp);

        for (Integer key : keys)
            checkValue(key, 62);

        tx = startTx(txMode);

        resMap = cache.invokeAll(keys, new ExceptionProcessor(null));

        if (tx != null)
            tx.commit();

        for (Integer key : keys) {
            final EntryProcessorResult<Integer> res = resMap.get(key);

            assertNotNull("No result for " + key);

            GridTestUtils.assertThrows(log, new Callable<Void>() {
                @Override public Void call() throws Exception {
                    res.get();

                    return null;
                }
            }, EntryProcessorException.class, "Test processor exception.");
        }

        for (Integer key : keys)
            checkValue(key, 62);

        tx = startTx(txMode);

        Map<Integer, EntryProcessor<Integer, Integer, Integer>> invokeMap = new HashMap<>();

        for (Integer key : keys) {
            switch (key % 4) {
                case 0: invokeMap.put(key, new IncrementProcessor()); break;

                case 1: invokeMap.put(key, new RemoveProcessor(62)); break;

                case 2: invokeMap.put(key, new ArgumentsSumProcessor()); break;

                case 3: invokeMap.put(key, new ExceptionProcessor(62)); break;

                default:
                    fail();
            }
        }

        resMap = cache.invokeAll(invokeMap, 10, 20, 30);

        if (tx != null)
            tx.commit();

        for (Integer key : keys) {
            final EntryProcessorResult<Integer> res = resMap.get(key);

            switch (key % 4) {
                case 0: {
                    assertNotNull("No result for " + key);

                    assertEquals(62, (int)res.get());

                    checkValue(key, 63);

                    break;
                }

                case 1: {
                    assertNull(res);

                    checkValue(key, null);

                    break;
                }

                case 2: {
                    assertNotNull("No result for " + key);

                    assertEquals(3, (int)res.get());

                    checkValue(key, 122);

                    break;
                }

                case 3: {
                    assertNotNull("No result for " + key);

                    GridTestUtils.assertThrows(log, new Callable<Void>() {
                        @Override public Void call() throws Exception {
                            res.get();

                            return null;
                        }
                    }, EntryProcessorException.class, "Test processor exception.");

                    checkValue(key, 62);

                    break;
                }
            }
        }

        cache.invokeAll(keys, new IncrementProcessor());

        tx = startTx(txMode);

        resMap = cache.invokeAll(keys, new RemoveProcessor(null));

        if (tx != null)
            tx.commit();

        assertEquals("Unexpected results: " + resMap, 0, resMap.size());

        for (Integer key : keys)
            checkValue(key, null);

        IgniteCache<Integer, Integer> asyncCache = cache.enableAsync();

        assertTrue(asyncCache.isAsync());

        assertNull(asyncCache.invokeAll(keys, new IncrementProcessor()));

        IgniteFuture<Map<Integer, EntryProcessorResult<Integer>>> fut = asyncCache.future();

        resMap = fut.get();

        exp = new HashMap<>();

        for (Integer key : keys)
            exp.put(key, -1);

        checkResult(resMap, exp);

        for (Integer key : keys)
            checkValue(key, 1);

        invokeMap = new HashMap<>();

        for (Integer key : keys)
            invokeMap.put(key, incProcessor);

        assertNull(asyncCache.invokeAll(invokeMap));

        fut = asyncCache.future();

        resMap = fut.get();

        for (Integer key : keys)
            exp.put(key, 1);

        checkResult(resMap, exp);

        for (Integer key : keys)
            checkValue(key, 2);
    }

    /**
     * @param resMap Result map.
     * @param exp Expected results.
     */
    private void checkResult(Map<Integer, EntryProcessorResult<Integer>> resMap, Map<Object, Object> exp) {
        assertNotNull(resMap);

        assertEquals(exp.size(), resMap.size());

        for (Map.Entry<Object, Object> expVal : exp.entrySet()) {
            EntryProcessorResult<Integer> res = resMap.get(expVal.getKey());

            assertNotNull("No result for " + expVal.getKey());

            assertEquals("Unexpected result for " + expVal.getKey(), res.get(), expVal.getValue());
        }
    }

    /**
     * @param key Key.
     * @param expVal Expected value.
     */
    protected void checkValue(Object key, @Nullable Object expVal) {
        if (expVal != null) {
            for (int i = 0; i < gridCount(); i++) {
                IgniteCache<Object, Object> cache = jcache(i);

                Object val = cache.localPeek(key);

                if (val == null)
                    assertFalse(cache(0).affinity().isPrimaryOrBackup(ignite(i).cluster().localNode(), key));
                else
                    assertEquals("Unexpected value for grid " + i, expVal, val);
            }
        }
        else {
            for (int i = 0; i < gridCount(); i++) {
                IgniteCache<Object, Object> cache = jcache(i);

                assertNull("Unexpected non null value for grid " + i, cache.localPeek(key));
            }
        }
    }

    /**
     * @return Test keys.
     * @throws Exception If failed.
     */
    protected Collection<Integer> keys() throws Exception {
        GridCache<Integer, Object> cache = cache(0);

        ArrayList<Integer> keys = new ArrayList<>();

        keys.add(primaryKeys(cache, 1, lastKey).get(0));

        if (gridCount() > 1) {
            keys.add(backupKeys(cache, 1, lastKey).get(0));

            if (cache.configuration().getCacheMode() != REPLICATED)
                keys.add(nearKeys(cache, 1, lastKey).get(0));
        }

        lastKey = Collections.max(keys) + 1;

        return keys;
    }

    /**
     * @param txMode Transaction concurrency mode.
     * @return Transaction.
     */
    @Nullable private IgniteTx startTx(@Nullable IgniteTxConcurrency txMode) {
        return txMode == null ? null : ignite(0).transactions().txStart(txMode, REPEATABLE_READ);
    }

    /**
     *
     */
    private static class ArgumentsSumProcessor implements EntryProcessor<Integer, Integer, Integer> {
        /** {@inheritDoc} */
        @Override public Integer process(MutableEntry<Integer, Integer> e, Object... args)
            throws EntryProcessorException {
            assertEquals(3, args.length);
            assertEquals(10, args[0]);
            assertEquals(20, args[1]);
            assertEquals(30, args[2]);

            assertTrue(e.exists());

            Integer res = e.getValue();

            for (Object arg : args)
                res += (Integer)arg;

            e.setValue(res);

            return args.length;
        }
    }

    /**
     *
     */
    protected static class ToStringProcessor implements EntryProcessor<Integer, Integer, String> {
        /** {@inheritDoc} */
        @Override public String process(MutableEntry<Integer, Integer> e, Object... arguments)
            throws EntryProcessorException {
            return String.valueOf(e.getValue());
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(ToStringProcessor.class, this);
        }
    }

    /**
     *
     */
    protected static class IncrementProcessor implements EntryProcessor<Integer, Integer, Integer> {
        /** {@inheritDoc} */
        @Override public Integer process(MutableEntry<Integer, Integer> e,
            Object... arguments) throws EntryProcessorException {
            if (e.exists()) {
                Integer val = e.getValue();

                assertNotNull(val);

                e.setValue(val + 1);

                assertTrue(e.exists());

                assertEquals(val + 1, (int) e.getValue());

                return val;
            }
            else {
                e.setValue(1);

                return -1;
            }
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(IncrementProcessor.class, this);
        }
    }

    /**
     *
     */
    private static class RemoveProcessor implements EntryProcessor<Integer, Integer, Integer> {
        /** */
        private Integer expVal;

        /**
         * @param expVal Expected value.
         */
        RemoveProcessor(@Nullable Integer expVal) {
            this.expVal = expVal;
        }

        /** {@inheritDoc} */
        @Override public Integer process(MutableEntry<Integer, Integer> e,
            Object... arguments) throws EntryProcessorException {
            assertTrue(e.exists());

            if (expVal != null)
                assertEquals(expVal, e.getValue());

            e.remove();

            assertFalse(e.exists());

            return null;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(RemoveProcessor.class, this);
        }
    }

    /**
     *
     */
    private static class ExceptionProcessor implements EntryProcessor<Integer, Integer, Integer> {
        /** */
        private Integer expVal;

        /**
         * @param expVal Expected value.
         */
        ExceptionProcessor(@Nullable Integer expVal) {
            this.expVal = expVal;
        }

        /** {@inheritDoc} */
        @Override public Integer process(MutableEntry<Integer, Integer> e,
            Object... arguments) throws EntryProcessorException {
            assertTrue(e.exists());

            if (expVal != null)
                assertEquals(expVal, e.getValue());

            throw new EntryProcessorException("Test processor exception.");
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(ExceptionProcessor.class, this);
        }
    }
}