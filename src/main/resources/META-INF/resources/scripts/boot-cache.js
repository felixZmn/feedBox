"use strict";

// Early paint is intentional no-op for cache v4+: rendered HTML is no longer
// stored (XSS). Modules restore from JSON via renderFoldersList.
(function () {})();
