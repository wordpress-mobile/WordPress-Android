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

import android.content.Intent;
import android.util.Log;
import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.commonsware.cwac.bus.AbstractBus;

abstract public class AsyncCache<K, V, B extends AbstractBus, M>
	extends CacheBase<K,V> {
	protected abstract V create(K key, M message, int forceStyle);
	
	private static String TAG="AsyncCache";
	private B bus=null;
	
	public AsyncCache(File cacheRoot, B bus, DiskCachePolicy policy,
										int maxSize) {
		super(cacheRoot, policy, maxSize);
		this.bus=bus;
	}
	
	public V get(K key, M message) {
		V result=super.get(key);
		
		if (result==null) {
			result=create(key, message, FORCE_NONE);
			super.put(key, result);
		}
		
		return(result);
	}
	
	public void forceLoad(K key, M message, int forceStyle) {
		super.remove(key);
		super.put(key, create(key, message, forceStyle));
	}
	
	protected B getBus() {
		return(bus);
	}
}