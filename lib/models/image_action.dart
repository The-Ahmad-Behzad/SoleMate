import '../models/image_filter.dart';

/// Types of actions that can be undone/redone
enum ImageActionType {
  delete,
  filterApply,
  filterRemove,
  reorder,
}

/// Represents an action that can be undone/redone in the Image Manager.
/// Each action stores enough data to reverse itself.
class ImageAction {
  final ImageActionType type;
  final DateTime timestamp;
  
  // For delete actions: stores the deleted path and its position
  final String? deletedPath;
  final int? deletedIndex;
  
  // For filter actions: stores the path and the previous filter
  final String? imagePath;
  final ImageFilter? previousFilter;
  final ImageFilter? newFilter;
  
  // For reorder actions: stores the old and new positions
  final int? fromIndex;
  final int? toIndex;

  ImageAction._({
    required this.type,
    required this.timestamp,
    this.deletedPath,
    this.deletedIndex,
    this.imagePath,
    this.previousFilter,
    this.newFilter,
    this.fromIndex,
    this.toIndex,
  });

  /// Create a delete action
  factory ImageAction.delete({
    required String path,
    required int index,
  }) {
    return ImageAction._(
      type: ImageActionType.delete,
      timestamp: DateTime.now(),
      deletedPath: path,
      deletedIndex: index,
    );
  }

  /// Create a filter apply action
  factory ImageAction.filterApply({
    required String path,
    required ImageFilter previousFilter,
    required ImageFilter newFilter,
  }) {
    return ImageAction._(
      type: ImageActionType.filterApply,
      timestamp: DateTime.now(),
      imagePath: path,
      previousFilter: previousFilter,
      newFilter: newFilter,
    );
  }

  /// Create a filter remove action (same as apply but distinct type)
  factory ImageAction.filterRemove({
    required String path,
    required ImageFilter previousFilter,
  }) {
    return ImageAction._(
      type: ImageActionType.filterRemove,
      timestamp: DateTime.now(),
      imagePath: path,
      previousFilter: previousFilter,
      newFilter: ImageFilter.none,
    );
  }

  /// Create a reorder action
  factory ImageAction.reorder({
    required int fromIndex,
    required int toIndex,
  }) {
    return ImageAction._(
      type: ImageActionType.reorder,
      timestamp: DateTime.now(),
      fromIndex: fromIndex,
      toIndex: toIndex,
    );
  }

  /// Human-readable description of this action
  String get description {
    switch (type) {
      case ImageActionType.delete:
        return 'Delete image';
      case ImageActionType.filterApply:
        return 'Apply ${newFilter?.name ?? "filter"}';
      case ImageActionType.filterRemove:
        return 'Remove filter';
      case ImageActionType.reorder:
        return 'Reorder images';
    }
  }

  @override
  String toString() => 'ImageAction($type at $timestamp)';
}

/// Manages the undo/redo history stack.
/// Limits history to [maxHistorySize] actions.
class ActionHistoryManager {
  static const int maxHistorySize = 10;

  final List<ImageAction> _undoStack = [];
  final List<ImageAction> _redoStack = [];

  /// Whether there are actions that can be undone
  bool get canUndo => _undoStack.isNotEmpty;

  /// Whether there are actions that can be redone
  bool get canRedo => _redoStack.isNotEmpty;

  /// Number of actions in undo stack
  int get undoCount => _undoStack.length;

  /// Number of actions in redo stack
  int get redoCount => _redoStack.length;

  /// Get the most recent action that can be undone (for tooltip)
  ImageAction? get nextUndoAction => _undoStack.isNotEmpty ? _undoStack.last : null;

  /// Get the most recent action that can be redone (for tooltip)
  ImageAction? get nextRedoAction => _redoStack.isNotEmpty ? _redoStack.last : null;

  /// Record a new action to the undo stack.
  /// Clears the redo stack since we're on a new branch.
  void recordAction(ImageAction action) {
    _undoStack.add(action);
    _redoStack.clear(); // New action invalidates redo history
    
    // Trim to max size
    while (_undoStack.length > maxHistorySize) {
      _undoStack.removeAt(0);
    }
  }

  /// Pop the most recent action for undo, push it to redo stack
  ImageAction? popForUndo() {
    if (_undoStack.isEmpty) return null;
    final action = _undoStack.removeLast();
    _redoStack.add(action);
    return action;
  }

  /// Pop the most recent action for redo, push it back to undo stack
  ImageAction? popForRedo() {
    if (_redoStack.isEmpty) return null;
    final action = _redoStack.removeLast();
    _undoStack.add(action);
    return action;
  }

  /// Clear all history
  void clear() {
    _undoStack.clear();
    _redoStack.clear();
  }
}
