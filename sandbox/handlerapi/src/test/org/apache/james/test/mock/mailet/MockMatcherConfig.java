/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/
package org.apache.james.test.mock.mailet;

import org.apache.mailet.MailetContext;
import org.apache.mailet.MatcherConfig;

/**
 * MatcherConfig
 */
public class MockMatcherConfig implements MatcherConfig {

    private String matcherName;

    private MailetContext mc;

    public MockMatcherConfig(String matcherName, MailetContext mc) {
        super();
        this.matcherName = matcherName;
        this.mc = mc;
    }

    public String getCondition() {
        if (matcherName.indexOf("=") >= 0) {
            return matcherName.substring(getMatcherName().length() + 1);
        } else {
            return null;
        }
    }

    public MailetContext getMailetContext() {
        return mc;
    }

    public String getMatcherName() {
        if (matcherName.indexOf("=") >= 0) {
            return matcherName.split("=")[0];
        } else {
            return matcherName;
        }
    }

}
