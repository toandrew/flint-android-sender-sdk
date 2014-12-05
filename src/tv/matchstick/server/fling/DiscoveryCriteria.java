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

package tv.matchstick.server.fling;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import tv.matchstick.fling.FlingMediaControlIntent;
import tv.matchstick.server.common.checker.ObjEqualChecker;
import android.text.TextUtils;

public final class DiscoveryCriteria {
    public String mCategory;
    public String mAppid;
    public final Set<String> mNamespaceList = new HashSet<String>();

    public DiscoveryCriteria() {
    }

    public static DiscoveryCriteria getDiscoveryCriteria(String category) {
        if (!category.equals(FlingMediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                && !category.equals(FlingMediaControlIntent.CATEGORY_FLING)
                && !category.startsWith(FlingMediaControlIntent.CATEGORY_FLING
                        + "/")
                && !category.startsWith(FlingMediaControlIntent.CATEGORY_FLING
                        + "/")) {
            throw new IllegalArgumentException(
                    "Invalid discovery control category:" + category);
        }

        DiscoveryCriteria criteria = new DiscoveryCriteria();
        criteria.mCategory = category;
        String parts[] = TextUtils.split(category, "/");
        switch (parts.length) {
        case 1:
            break;
        case 2:
            criteria.mAppid = parts[1];
            break;
        case 3:
            if (!TextUtils.isEmpty(parts[1])) {
                criteria.mAppid = parts[1];
            }

            java.util.List list = Arrays.asList(TextUtils.split(parts[2], ","));
            checkNamespaces(((Collection) (list)));
            criteria.mNamespaceList.addAll(list);
            break;
        default:
            throw new IllegalArgumentException(
                    "Could not parse criteria from control category: "
                            + category);
        }

        return criteria;
    }

    private static void checkNamespaces(Collection namespaces) {
        if (namespaces != null && namespaces.size() > 0) {
            Iterator<String> iterator = namespaces.iterator();
            while (iterator.hasNext()) {
                String namespace = (String) iterator.next();
                if (TextUtils.isEmpty(namespace) || namespace.trim().equals("")) {
                    throw new IllegalArgumentException(
                            "Namespaces must not be null or empty");
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "Must specify at least one namespace");
        }
    }

    @Override
    public final boolean equals(Object obj) {
        boolean isEqual = true;
        if (obj == null || !(obj instanceof DiscoveryCriteria)) {
            isEqual = false;
        } else if (obj != this) {
            DiscoveryCriteria criteria = (DiscoveryCriteria) obj;
            if (!ObjEqualChecker.isEquals(mCategory, criteria.mCategory)
                    || !ObjEqualChecker.isEquals(mAppid, criteria.mAppid)
                    || !ObjEqualChecker.isEquals(mNamespaceList,
                            criteria.mNamespaceList))
                return false;
        }
        return isEqual;
    }

    @Override
    public final int hashCode() {
        Object aobj[] = new Object[3];
        aobj[0] = mCategory;
        aobj[1] = mAppid;
        aobj[2] = mNamespaceList;
        return Arrays.hashCode(aobj);
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DiscoveryCriteria(category=").append(mCategory)
                .append("; appid=").append(mAppid).append("; ns=")
                .append(TextUtils.join(",", mNamespaceList)).append(")");
        return sb.toString();
    }
}
