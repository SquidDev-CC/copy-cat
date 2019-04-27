import { h, render } from "preact";
import { Main } from "./main";

// Start the window!
const page = document.getElementById("page") as HTMLElement;
render(<Main />, page, page.lastElementChild || undefined);
