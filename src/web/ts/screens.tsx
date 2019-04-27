import { h } from "preact";

const githubLink =
  <div class="info-description">
    <p>
      Think you've found a bug? Have a suggestion? Why not put it
      on <a href="https://github.com/SquidDev-CC/" title="The GitHub repository">the GitHub repo</a>?
    </p>
  </div>;

export type UnknownErrorProps = {
  error: string;
};

export const UnknownError = ({ error }: UnknownErrorProps) =>
  <div class="info-container">
    <div class="info-view error-view">
      <h2>An error occured</h2>
      <pre>{error}</pre>
    </div>
    {githubLink}
  </div>;
