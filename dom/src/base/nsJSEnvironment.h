/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*-
 *
 * The contents of this file are subject to the Netscape Public License
 * Version 1.0 (the "NPL"); you may not use this file except in
 * compliance with the NPL.  You may obtain a copy of the NPL at
 * http://www.mozilla.org/NPL/
 *
 * Software distributed under the NPL is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the NPL
 * for the specific language governing rights and limitations under the
 * NPL.
 *
 * The Initial Developer of this code under the NPL is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1998 Netscape Communications Corporation.  All Rights
 * Reserved.
 */
#ifndef nsJSEnvironment_h___
#define nsJSEnvironment_h___

#include "nsIScriptContext.h"
#include "nsCOMPtr.h"
#include "jsapi.h"
#include "nsCOMPtr.h"

class nsIScriptSecurityManager;
// XXXbe vc5 sucks: must include nsIScriptNameSpaceManager.h to use nsCOMPtr with it
#if 0
class nsIScriptNameSpaceManager;
#else
#include "nsIScriptNameSpaceManager.h"
#endif
class nsIPrincipal;

class nsJSContext : public nsIScriptContext {
private:
  JSContext *mContext;
  nsCOMPtr<nsIScriptNameSpaceManager> mNameSpaceManager;
  PRBool mIsInitialized;
  PRUint32 mNumEvaluations;
  nsIScriptSecurityManager* mSecurityManager; /* XXXbe nsCOMPtr to service */
  nsIScriptContextOwner* mOwner;  /* NB: weak reference, not ADDREF'd */
  nsScriptTerminationFunc mTerminationFunc;
  nsCOMPtr<nsISupports> mRef;
  
public:
  nsJSContext(JSRuntime *aRuntime);
  virtual ~nsJSContext();

  NS_DECL_ISUPPORTS

  NS_IMETHOD       EvaluateString(const nsString& aScript,
                                  const char *aURL,
                                  PRUint32 aLineNo,
                                  const char* aVersion,
                                  nsString& aRetValue,
                                  PRBool* aIsUndefined);
  NS_IMETHOD       EvaluateString(const nsString& aScript,
                                  void *aObj,
                                  nsIPrincipal *principal,
                                  const char *aURL,
                                  PRUint32 aLineNo,
                                  const char* aVersion,
                                  nsString& aRetValue,
                                  PRBool* aIsUndefined);
  NS_IMETHOD       CompileFunction(void *aObj, nsIAtom *aName,
                                   const nsString& aBody);
  NS_IMETHOD       CallFunction(void *aObj, void *aFunction, 
                                PRUint32 argc, void *argv, 
                                PRBool *aBoolResult);
  NS_IMETHOD SetDefaultLanguageVersion(const char* aVersion);
  NS_IMETHOD_(nsIScriptGlobalObject*)    GetGlobalObject();
  NS_IMETHOD_(void*)                     GetNativeContext();
  NS_IMETHOD     InitClasses();
  NS_IMETHOD     InitContext(nsIScriptGlobalObject *aGlobalObject);
  NS_IMETHOD     IsContextInitialized();
  NS_IMETHOD     AddNamedReference(void *aSlot, void *aScriptObject,
                                         const char *aName);
  NS_IMETHOD     RemoveReference(void *aSlot, void *aScriptObject);
  NS_IMETHOD     GC();
  NS_IMETHOD GetNameSpaceManager(nsIScriptNameSpaceManager** aInstancePtr);
  NS_IMETHOD GetSecurityManager(nsIScriptSecurityManager** aInstancePtr);

  NS_IMETHOD ScriptEvaluated(void);
  NS_IMETHOD SetOwner(nsIScriptContextOwner* owner);
  NS_IMETHOD GetOwner(nsIScriptContextOwner** owner);
  NS_IMETHOD SetTerminationFunction(nsScriptTerminationFunc aFunc,
                                    nsISupports* aRef);

  nsresult InitializeExternalClasses();
  nsresult InitializeLiveConnectClasses();
};

class nsIJSRuntimeService;

class nsJSEnvironment {
private:
  JSRuntime *mRuntime;
  nsIJSRuntimeService* mRuntimeService; /* XXXbe nsCOMPtr to service */

public:
  static nsJSEnvironment *sTheEnvironment;

  nsJSEnvironment();
  ~nsJSEnvironment();

  nsIScriptContext* GetNewContext();

  static nsJSEnvironment *GetScriptingEnvironment();

};

/* prototypes */
void PR_CALLBACK NS_ScriptErrorReporter(JSContext *cx, const char *message, JSErrorReport *report);

#endif /* nsJSEnvironment_h___ */
