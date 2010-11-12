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

package com.commonsware.cwac.thumbnail;

import com.commonsware.cwac.bus.AbstractBus;

public class ThumbnailBus
	extends AbstractBus<ThumbnailMessage, String, ThumbnailBus.MatchStrategy> {
	
	public ThumbnailBus() {
		super();
		
		setStrategy(new MatchStrategy());
	}
	
	public ThumbnailMessage createMessage(String key) {
		return(new ThumbnailMessage(key));
	}
	
	class MatchStrategy
		implements AbstractBus.Strategy<ThumbnailMessage, String> {
		public boolean isMatch(ThumbnailMessage message, String filter) {
			return(filter!=null && message!=null &&
						 filter.equals(message.getKey()));
		}
	}
}