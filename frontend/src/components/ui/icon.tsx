import type { SVGProps } from "react";

const paths = {
  home: "m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z",
  users:
    "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M16 3a4 4 0 0 1 0 8M22 21v-2a4 4 0 0 0-3-3.87M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z",
  briefcase: "M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2M3 6h18v14H3ZM3 11a20 20 0 0 0 18 0M12 10v4",
  target: "M21 12a9 9 0 1 1-9-9M17 12a5 5 0 1 1-5-5M12 12l9-9M16 3h5v5",
  layers: "m12 3 10 5-10 5L2 8Zm-10 9 10 5 10-5M2 16l10 5 10-5",
  message: "M21 11.5a8.5 8.5 0 0 1-8.5 8.5H3l2-5a8.5 8.5 0 1 1 16-3.5ZM8 10h8M8 14h5",
  list: "m3 6 1 1 2-2M10 6h11M3 12l1 1 2-2M10 12h11M3 18l1 1 2-2M10 18h11",
  clipboard: "M9 5H5v16h14V5h-4M9 3h6v4H9ZM8 12h8M8 16h5",
  file: "M14 2H5v20h14V7Zm0 0v5h5M8 12h8M8 16h6",
  upload: "M12 16V3m-5 5 5-5 5 5M4 16v5h16v-5",
  award: "M16 9a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM8 13l-2 9 6-3 6 3-2-9",
  arrow: "M5 12h14m-6-6 6 6-6 6",
  chevron: "m9 5 7 7-7 7",
  menu: "M4 6h16M4 12h16M4 18h16",
  close: "m6 6 12 12M6 18 18 6",
  info: "M12 11v6M12 7h.01M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z",
  check: "m8 12 3 3 5-6M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z",
  sun: "M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5 19 19M5 19l1.5-1.5M17.5 6.5 19 5",
  moon: "M21 13a9 9 0 1 1-10-10 7 7 0 0 0 10 10Z",
  monitor: "M3 4h18v13H3ZM8 21h8M12 17v4",
  // Session icons exported from Figma: yuMkPEz9GT1FyiyOu2vrka, nodes 7:19 and 7:22.
  login: "M9 7.5C9.3 4.6 11 3 14 3H15C19 3 21 5 21 9V15C21 19 19 21 15 21H14C11 21 9.3 19.4 9 16.5M2 12H15M12 15L15 12L12 9",
  logout: "M15 7.5C14.7 4.6 13 3 10 3H9C5 3 3 5 3 9V15C3 19 5 21 9 21H10C13 21 14.7 19.4 15 16.5M9 12H22M19 15L22 12L19 9",
} as const;

export type IconName = keyof typeof paths;

export function Icon({ name, ...props }: SVGProps<SVGSVGElement> & { name: IconName }) {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      <path d={paths[name]} />
    </svg>
  );
}
