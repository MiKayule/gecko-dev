/* -*- Mode: C; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*-
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

#ifndef _MIMEMPAR_H_
#define _MIMEMPAR_H_

#include "mimemult.h"

/* The MimeMultipartParallel class implements the multipart/parallel MIME 
   container, which is currently no different from multipart/mixed, since
   it's not clear that there's anything useful it could do differently.
 */

typedef struct MimeMultipartParallelClass MimeMultipartParallelClass;
typedef struct MimeMultipartParallel      MimeMultipartParallel;

struct MimeMultipartParallelClass {
  MimeMultipartClass multipart;
};

extern MimeMultipartParallelClass mimeMultipartParallelClass;

struct MimeMultipartParallel {
  MimeMultipart multipart;
};

#endif /* _MIMEMPAR_H_ */
