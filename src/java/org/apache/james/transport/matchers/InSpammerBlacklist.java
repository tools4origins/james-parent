/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.transport.matchers;

import org.apache.mailet.GenericMatcher;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * Checks the network IP address of the sending server against a
 * blacklist of spammers.  There are 3 lists that support this...
 * <ul>
 * <li><b>blackholes.mail-abuse.org</b>: Rejected - see  http://www.mail-abuse.org/rbl/
 * <li><b>dialups.mail-abuse.org</b>: Dialup - see http://www.mail-abuse.org/dul/
 * <li><b>relays.mail-abuse.org</b>: Open spam relay - see http://www.mail-abuse.org/rss/
 * </ul>
 *
 * Example:
 * &lt;mailet match="InSpammerBlacklist=blackholes.mail-abuse.org" class="ToProcessor"&gt;
 *   &lt;processor&gt;spam&lt;/processor&gt;
 * &lt;/mailet&gt;
 *
 */
public class InSpammerBlacklist extends GenericMatcher {
    String network = null;

    public void init() throws MessagingException {
        network = getCondition();
    }

    public Collection match(Mail mail) {
        String host = mail.getRemoteAddr();
        try {
            //Have to reverse the octets first
            StringBuffer sb = new StringBuffer();
            StringTokenizer st = new StringTokenizer(host, " .", false);

            while (st.hasMoreTokens()) {
                sb.insert(0, st.nextToken() + ".");
            }

            //Add the network prefix for this blacklist
            sb.append(network);

            //Try to look it up
            InetAddress.getByName(sb.toString());

            //If we got here, that's bad... it means the host
            //  was found in the blacklist
            return mail.getRecipients();
        } catch (UnknownHostException uhe) {
            //This is good... it's not on the list
            return null;
        }
    }
}
