/***
	Copyright (c) 2008-2009 CommonsWare, LLC
	
	Licensed under the Apache License, Version 2.0 (the "License"); you may
	not use this file except in compliance with the License. You may obtain
	a copy of the License at
		http://www.apache.org/licenses/LICENSE-2.0
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package com.commonsware.cwac.cache;

import com.commonsware.cwac.task.AsyncTaskEx;

import android.util.Log;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

abstract public class CacheBase<K, V> {
	public static final int CACHE_MEMORY=1;
	public static final int CACHE_DISK=2;
	public static final int CACHE_NONE=3;
	public static final int FORCE_NONE=1;
	public static final int FORCE_SOFT=2;
	public static final int FORCE_HARD=3;
	private static String TAG="CacheBase";
	private File cacheRoot=null;
	private int maxSize=0;
	private Map<K, V> cache=Collections.synchronizedMap(new LinkedHashMap<K, V>(101, .75F, true) {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unchecked")
		protected boolean removeEldestEntry(Map.Entry eldest) {  
			return(size()>maxSize);  
		}  
  });
	
	public CacheBase(File cacheRoot, DiskCachePolicy policy,
										int maxSize) {
		this.cacheRoot=cacheRoot;
		this.maxSize=maxSize;
		
		if (cacheRoot!=null) {
			new CacheCleanTask().execute(policy);
		}
	}
	
	public V get(K key) {
		return(cache.get(key));
	}
	
	public int getStatus(K key) {
		if (cache.containsKey(key)) {
			return(CACHE_MEMORY);
		}
		
		return(CACHE_NONE);
	}
	
	protected void put(K key, V value) {
		cache.put(key, value);
	}
	
	public void remove(K key) {
		cache.remove(key);
	}
	
	protected File getCacheRoot() {
		return(cacheRoot);
	}
	
	public interface DiskCachePolicy {
		boolean eject(File cachedFile);
	}
	
	class CacheCleanTask extends AsyncTaskEx<DiskCachePolicy, Void, Void> {
		@Override
		protected Void doInBackground(DiskCachePolicy... policies) {
			try {
				walkDir(cacheRoot, policies[0]);
			}
			catch (Throwable t) {
				Log.e(TAG, "Exception cleaning cache", t);
			}
			
			return(null);
		}

		void walkDir(File dir, DiskCachePolicy policy) {
      if (dir.isDirectory()) {
        String[] children=dir.list();
    
		    for (int i=0; i<children.length; i++) {
          walkDir(new File(dir, children[i]), policy);
        }
      }
			else if (policy.eject(dir)) {
				dir.delete();
      }
    }
	}
}