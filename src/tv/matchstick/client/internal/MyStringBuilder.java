/*
 * Copyright (C) 2013-2014, Infthink (Beijing) Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package tv.matchstick.client.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyStringBuilder {

	public static boolean compare(Object a, Object b) {
		return ((a == b) || ((a != null) && (a.equals(b))));
	}

	public static int hashCode(Object[] objects) {
		return Arrays.hashCode(objects);
	}

	public static StringBuilderImpl newStringBuilder(Object root) {
		return new StringBuilderImpl(root);
	}

	public static final class StringBuilderImpl {
		private final List<String> msgList;
		private final Object rootObj;

		private StringBuilderImpl(Object root) {
			this.rootObj = ValueChecker.checkNullPointer(root);
			this.msgList = new ArrayList<String>();
		}

		public StringBuilderImpl append(String key, Object value) {
			this.msgList.add(ValueChecker.checkNullPointer(key) + "="
					+ String.valueOf(value));
			return this;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder(100).append(
					this.rootObj.getClass().getSimpleName()).append('{');
			int size = this.msgList.size();
			for (int i = 0; i < size; ++i) {
				sb.append((String) this.msgList.get(i));
				if (i >= size - 1) {
					continue;
				}
				sb.append(", ");
			}
			return sb.append('}').toString();
		}
	}

}
