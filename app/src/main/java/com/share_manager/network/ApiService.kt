package com.share_manager.network

// Retrofit is wired up in AppModule but the iporesult.cdsc.com.np API
// returns inconsistent JSON (sometimes lenient / non-standard) so all
// network calls in MeroShareRepository use raw OkHttp + manual Gson
// parsing for reliability. This interface is kept as a placeholder and
// can be used for future strictly-typed endpoints.

// (intentionally empty — remove file entirely if you never add endpoints here)
