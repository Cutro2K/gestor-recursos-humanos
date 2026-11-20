import { register } from "node:module";

register(import.meta.url, import.meta.url);

export async function resolve(specifier, context, nextResolve) {
  try {
    return await nextResolve(specifier, context);
  } catch (err) {
    if (err.code === "ERR_MODULE_NOT_FOUND" && (specifier.startsWith("./") || specifier.startsWith("../"))) {
      return await nextResolve(specifier + ".ts", context);
    }
    throw err;
  }
}
