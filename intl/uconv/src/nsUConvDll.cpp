/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*-
 *
 * The contents of this file are subject to the Netscape Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is Mozilla Communicator client code.
 *
 * The Initial Developer of the Original Code is Netscape Communications
 * Corporation.  Portions created by Netscape are Copyright (C) 1998
 * Netscape Communications Corporation.  All Rights Reserved.
 */

#include "nsRepository.h"
#include "nsICharsetConverterManager.h"
#include "nsIUnicodeEncoder.h"
#include "nsIUnicodeDecoder.h"
#include "nsCharsetConverterManager.h"
#include "nsAscii2Unicode.h"
#include "nsConverterCID.h"

//----------------------------------------------------------------------
// Global functions and data [declaration]

extern "C" PRInt32 g_InstanceCount = 0;
extern "C" PRInt32 g_LockCount = 0;

//----------------------------------------------------------------------
// Global functions and data [implementation]

NS_DEFINE_IID(kIFactoryIID, NS_IFACTORY_IID);

extern "C" NS_EXPORT PRBool NSCanUnload()
{
  return PRBool(g_InstanceCount == 0 && g_LockCount == 0);
}

extern "C" NS_EXPORT nsresult NSGetFactory(const nsCID &aCID, 
                                           nsIFactory **aFactory)
{
  if (aFactory == NULL) return NS_ERROR_NULL_POINTER;

  // the converter manager
  if (aCID.Equals(kCharsetConverterManagerCID)) {
    nsManagerFactory *factory = new nsManagerFactory();
    nsresult res = factory->QueryInterface(kIFactoryIID, (void **) aFactory);

    if (NS_FAILED(res)) {
      *aFactory = NULL;
      delete factory;
    }

    return res;
  }


  // the Ascii2Unicode converter
  if (aCID.Equals(kAscii2UnicodeCID)) {
    nsAscii2UnicodeFactory *factory = new nsAscii2UnicodeFactory();
    nsresult res = factory->QueryInterface(kIFactoryIID, (void **) aFactory);

    if (NS_FAILED(res)) {
      *aFactory = NULL;
      delete factory;
    }

    return res;
  }

  return NS_NOINTERFACE;
}

extern "C" NS_EXPORT nsresult NSRegisterSelf(const char * path)
{
  nsresult res;

  if (NS_FAILED(res)) return res;

  res = nsRepository::RegisterFactory(kAscii2UnicodeCID, path, 
      PR_TRUE, PR_TRUE);
  if (NS_FAILED(res)) return res;

  res = nsRepository::RegisterFactory(kCharsetConverterManagerCID, path, 
      PR_TRUE, PR_TRUE);
  return res;
}

extern "C" NS_EXPORT nsresult NSUnregisterSelf(const char * path)
{
  nsresult res;

  res = nsRepository::UnregisterFactory(kAscii2UnicodeCID, path);
  if (NS_FAILED(res)) return res;

  res = nsRepository::UnregisterFactory(kCharsetConverterManagerCID, path);
  return res;
}
