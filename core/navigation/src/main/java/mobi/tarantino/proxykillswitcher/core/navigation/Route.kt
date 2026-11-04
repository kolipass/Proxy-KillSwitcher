package mobi.tarantino.proxykillswitcher.core.navigation

/**
 * A feature's route implements exactly one of these to say which of the app's three independent
 * navigation surfaces it belongs to. Concrete routes are `@Serializable` data classes/objects,
 * declared in the feature module itself, e.g. `@Serializable data object Home : TabRoute`.
 */
interface TabRoute

interface FullscreenRoute

interface SheetRoute
