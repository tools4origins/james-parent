/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailboxmanager.torque.repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.commons.collections.IteratorUtils;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.GeneralMailbox;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.mock.TorqueMailboxManagerProviderSingleton;
import org.apache.james.mailboxmanager.redundant.AbstractMailRepositoryNativeTestCase;
import org.apache.james.mailboxmanager.repository.MailboxManagerMailRepository;

public class TorqueMailboxManagerMailRepositoryNativeTestCase extends
        AbstractMailRepositoryNativeTestCase {

    private static final String TUSER_INBOX = "#mail.tuser.INBOX";
    GeneralMailbox shadowMailbox = null;

    protected void configureRepository() throws Exception {
        TorqueMailboxManagerProviderSingleton.reset();
        MailboxManagerMailRepository mailboxManagerMailRepository = new MailboxManagerMailRepository();

        DefaultConfigurationBuilder db = new DefaultConfigurationBuilder();

        Configuration conf = db
                .build(
                        new ByteArrayInputStream(
                                ("<repository destinationURL=\"mailboxmanager://#mail/tuser\" postfix=\".INBOX\" translateDelimiters=\"true\" type=\"MAIL\"></repository>")
                                        .getBytes()), "repository");
        mailboxManagerMailRepository.configure(conf);
        mailboxManagerMailRepository.initialize();
        mailboxManagerMailRepository.setMailboxManagerProvider(TorqueMailboxManagerProviderSingleton
                .getTorqueMailboxManagerProviderInstance());
        mailRepository = mailboxManagerMailRepository;

    }

    protected void destroyRepository() throws IOException, MessagingException {
    }

    protected void assertNativeMessageCountEquals(int count) {
        assertEquals(count, getNativeMessageCount());
    }

    
    protected void assertNativeMessagesEqual(Collection expectedMessages)
            throws IOException, MessagingException {
        Collection existing = getNativeMessages();
        Set existingSet = new HashSet();
        for (Iterator iter = existing.iterator(); iter.hasNext();) {
            MimeMessage mm = (MimeMessage) iter.next();
            existingSet.add(new Integer(messageHashSum(mm)));
        }
        Set expectedSet = new HashSet();
        for (Iterator iter = expectedMessages.iterator(); iter.hasNext();) {
            MimeMessage mm = (MimeMessage) iter.next();
            expectedSet.add(new Integer(messageHashSum(mm)));
        }
        assertEquals(expectedSet.size(), existingSet.size());
        assertTrue(expectedSet.equals(existingSet));

    }

    protected int getNativeMessageCount() {
        try {
            return getShadowMailbox().getMessageCount();
        } catch (MailboxManagerException e) {
            throw new RuntimeException(e);
        }
    }

    public void testLock() throws MessagingException {
        super.testLock();
    }

    
    protected Collection getNativeMessages() {
        final Iterator it;
        try {
            it = getShadowMailbox().getMessages(GeneralMessageSetImpl.all(),
                    MessageResult.MIME_MESSAGE);

        } catch (MailboxManagerException e) {
            throw new RuntimeException(e);
        }
        Collection existing = IteratorUtils.toList(it);
        return existing;
    }

    protected void nativeStoreMessage(MimeMessage mm) {
        try {
            getShadowMailbox().appendMessage(mm, new Date(),
                    MessageResult.NOTHING);
        } catch (MailboxManagerException e) {
            throw new RuntimeException(e);
        }

    }

    protected GeneralMailbox getShadowMailbox() {
        if (shadowMailbox == null) {
            try {
                MailboxManager mailboxManager= TorqueMailboxManagerProviderSingleton
                .getTorqueMailboxManagerProviderInstance()
                .getMailboxManagerInstance();
                if (!mailboxManager.existsMailbox(TUSER_INBOX)) {
                    mailboxManager.createMailbox(TUSER_INBOX);
                }
                shadowMailbox = mailboxManager.getGeneralMailbox(TUSER_INBOX);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return shadowMailbox;
    }

}
