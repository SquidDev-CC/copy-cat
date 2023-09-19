export const classNames = (...classes: Array<string | undefined>): string => classes.filter(x => !!x).join(" ");
