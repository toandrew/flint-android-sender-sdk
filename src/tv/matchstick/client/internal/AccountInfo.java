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
import java.util.Collection;
import java.util.List;

import tv.matchstick.client.common.internal.safeparcel.ParcelReadUtil;
import tv.matchstick.client.common.internal.safeparcel.ParcelWriteUtil;
import tv.matchstick.client.common.internal.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

public class AccountInfo {

	private final AccountInfoData mData;
	private final View mView;

	public AccountInfo(String accountName,
			Collection<String> scopeUriCollection, int gravityForPopups,
			View view, String packageName) {
		mData = new AccountInfoData(accountName, scopeUriCollection,
				gravityForPopups, packageName);
		mView = view;
	}

	public String getAccountName() {
		return mData.getAccountName();
	}

	public String getAccountNameNotNull() {
		return mData.getAccountNameNotNull();
	}

	public int getGravityForPopups() {
		return mData.getGravityForPopups();
	}

	public List<String> copyScopeUriList() {
		return mData.copyScopeUriList();
	}

	public String[] convertListToArray() {
		return mData.copyScopeUriList().toArray(new String[0]);
	}

	public String getPackageName() {
		return mData.getPackageName();
	}

	public View getView() {
		return mView;
	}

	public static final class AccountInfoData implements SafeParcelable {
		public static final Parcelable.Creator<AccountInfoData> CREATOR = new Parcelable.Creator<AccountInfoData>() {

			@Override
			public AccountInfoData createFromParcel(Parcel source) {
				// TODO Auto-generated method stub

				int size = ParcelReadUtil.readStart(source);
				int version = 0;
				String name = null;
				ArrayList<String> scopeUriList = null;
				int gravity = 0;
				String packageName = null;
				while (source.dataPosition() < size) {
					int type = ParcelReadUtil.readSingleInt(source);
					switch (ParcelReadUtil.halfOf(type)) {
					case 1:
						name = ParcelReadUtil.readString(source, type);
						break;
					case 1000:
						version = ParcelReadUtil.readInt(source, type);
						break;
					case 2:
						scopeUriList = ParcelReadUtil.readStringList(source,
								type);
						break;
					case 3:
						gravity = ParcelReadUtil.readInt(source, type);
						break;
					case 4:
						packageName = ParcelReadUtil.readString(source, type);
						break;
					default:
						ParcelReadUtil.skip(source, type);
					}
				}

				if (source.dataPosition() != size) {
					throw new ParcelReadUtil.SafeParcel(
							"Overread allowed size end=" + size, source);
				}

				return new AccountInfoData(version, name, scopeUriList,
						gravity, packageName);
			}

			@Override
			public AccountInfoData[] newArray(int size) {
				// TODO Auto-generated method stub

				return new AccountInfoData[size];
			}

		};

		private final int mVersionCode;
		private final String mAccountName;
		private final List<String> mScopeUriList;
		private final int mGravityForPopups;
		private final String mPackageName;

		AccountInfoData(int versionCode, String accountName,
				List<String> scopeUriList, int gravityForPopups,
				String packageName) {
			mScopeUriList = new ArrayList<String>();
			mScopeUriList.addAll(scopeUriList);

			mVersionCode = versionCode;
			mAccountName = accountName;

			mGravityForPopups = gravityForPopups;
			mPackageName = packageName;
		}

		public AccountInfoData(String accountName,
				Collection<String> scopeUriCollection, int gravityForPopups,
				String packageName) {
			this(3, accountName, new ArrayList<String>(scopeUriCollection),
					gravityForPopups, packageName);
		}

		public String getAccountName() {
			return mAccountName;
		}

		public String getAccountNameNotNull() {
			return ((mAccountName != null) ? mAccountName
					: "<<default account>>");
		}

		public int getGravityForPopups() {
			return mGravityForPopups;
		}

		public String getPackageName() {
			return mPackageName;
		}

		public List<String> copyScopeUriList() {
			return new ArrayList<String>(mScopeUriList);
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			buildParcel(out, flags);
		}

		public int getVersionCode() {
			return mVersionCode;
		}

		private void buildParcel(Parcel out, int flags) {
			int i = ParcelWriteUtil.position(out);
			ParcelWriteUtil.write(out, 1, getAccountName(), false);
			ParcelWriteUtil.write(out, 1000, getVersionCode());
			ParcelWriteUtil.writeStringList(out, 2, copyScopeUriList(), false);
			ParcelWriteUtil.write(out, 3, getGravityForPopups());
			ParcelWriteUtil.write(out, 4, getPackageName(), false);
			ParcelWriteUtil.writeEnd(out, i);
		}
	}

}
