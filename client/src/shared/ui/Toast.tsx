export function Toast({ message }: { message: string }) {
  return (
    <div className="toast toast-end z-50">
      <div role="status" className="alert alert-success shadow-lg">
        <span>{message}</span>
      </div>
    </div>
  );
}
