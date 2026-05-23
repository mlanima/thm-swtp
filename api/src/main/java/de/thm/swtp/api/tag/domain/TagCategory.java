package de.thm.swtp.api.tag.domain;
/** Categorizes of a tag.
 * Used to group tags of projects and users.
 */
public enum TagCategory {
    /** Programming languages, e.g. Java or TypeScript.*/
    PROGRAMMING_LANGUAGE,

    /** Frontend technologies, e.g. Angular or Tailwind CSS.*/
    FRONTEND_TECHNOLOGY,

    /** Backend technologies, e.g. Spring Boot or Node.js. */
    BACKEND_TECHNOLOGY,

    /** Database technologies, e.g. Postgres or MySQL. */
    DATABASE,

    /** DevOps/deployment technologies, e.g. Docker or CI/CD tools. */
    DEVOPS,

    /** Domain of the project, e.g. Machine Learning or Web development. */
    DOMAIN,

    /** Development tool, e.g. Git */
    TOOL,

}
