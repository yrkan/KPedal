/**
 * Number formatting utilities to reduce API response payload size
 * Rounds floats to 1 decimal place (saves ~20% bandwidth)
 */

/** Round to 1 decimal place */
export const round1 = (n: number | null | undefined): number => {
  if (n === null || n === undefined) return 0;
  return Math.round(n * 10) / 10;
};

/** Round to integer */
export const roundInt = (n: number | null | undefined): number => {
  if (n === null || n === undefined) return 0;
  return Math.round(n);
};

/** Round all numeric values in an object (shallow), preserving type */
export function roundObjectValues<T extends object>(obj: T, decimals: 1 | 0 = 1): T {
  const result = { ...obj } as T;
  const rounder = decimals === 1 ? round1 : roundInt;

  for (const key in result) {
    const value = (result as Record<string, unknown>)[key];
    if (typeof value === 'number') {
      (result as Record<string, unknown>)[key] = rounder(value);
    }
  }

  return result;
}

/** Round numeric values in array of objects, preserving type */
export function roundArrayValues<T extends object>(arr: T[], decimals: 1 | 0 = 1): T[] {
  return arr.map((item) => roundObjectValues(item, decimals));
}
